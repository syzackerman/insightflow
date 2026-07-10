package com.sophia.insightflow.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

  private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

  private final ObjectMapper objectMapper;
  private final byte[] secret;
  private final long expirationMinutes;

  public JwtTokenService(
      ObjectMapper objectMapper,
      @Value("${insightflow.security.jwt-secret}") String secret,
      @Value("${insightflow.security.jwt-expiration-minutes}") long expirationMinutes) {
    this.objectMapper = objectMapper;
    this.secret = secret.getBytes(StandardCharsets.UTF_8);
    this.expirationMinutes = expirationMinutes;
  }

  public String createToken(UserPrincipal principal) {
    Instant now = Instant.now();
    Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
    Map<String, Object> claims = new LinkedHashMap<>();
    claims.put("sub", principal.id().toString());
    claims.put("email", principal.email());
    claims.put("name", principal.displayName());
    claims.put("role", principal.role());
    claims.put("iat", now.getEpochSecond());
    claims.put("exp", now.plusSeconds(expirationMinutes * 60).getEpochSecond());

    String unsigned = encodeJson(header) + "." + encodeJson(claims);
    return unsigned + "." + sign(unsigned);
  }

  public Optional<UserPrincipal> parseToken(String token) {
    if (token == null || token.isBlank()) {
      return Optional.empty();
    }
    String[] parts = token.split("\\.");
    if (parts.length != 3) {
      return Optional.empty();
    }
    String unsigned = parts[0] + "." + parts[1];
    if (!constantTimeEquals(sign(unsigned), parts[2])) {
      return Optional.empty();
    }

    try {
      JsonNode header =
          objectMapper.readTree(new String(URL_DECODER.decode(parts[0]), StandardCharsets.UTF_8));
      if (!"HS256".equals(header.path("alg").asText())
          || !"JWT".equals(header.path("typ").asText())) {
        return Optional.empty();
      }
      JsonNode claims =
          objectMapper.readTree(new String(URL_DECODER.decode(parts[1]), StandardCharsets.UTF_8));
      if (claims.path("exp").asLong(0) < Instant.now().getEpochSecond()) {
        return Optional.empty();
      }
      return Optional.of(
          new UserPrincipal(
              UUID.fromString(claims.path("sub").asText()),
              claims.path("email").asText(),
              claims.path("name").asText(),
              claims.path("role").asText("USER")));
    } catch (IllegalArgumentException | JsonProcessingException error) {
      return Optional.empty();
    }
  }

  private String encodeJson(Object value) {
    try {
      return URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
    } catch (JsonProcessingException error) {
      throw new IllegalStateException("Could not encode JWT", error);
    }
  }

  private String sign(String unsignedToken) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret, "HmacSHA256"));
      return URL_ENCODER.encodeToString(mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception error) {
      throw new IllegalStateException("Could not sign JWT", error);
    }
  }

  private boolean constantTimeEquals(String expected, String actual) {
    byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
    byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);
    return MessageDigest.isEqual(expectedBytes, actualBytes);
  }
}
