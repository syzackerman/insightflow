package com.sophia.insightflow.auth;

import com.sophia.insightflow.auth.dto.AuthResponse;
import com.sophia.insightflow.auth.dto.LoginRequest;
import com.sophia.insightflow.auth.dto.RegisterRequest;
import com.sophia.insightflow.auth.dto.UserProfileResponse;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private final UserAccountRepository userAccountRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenService jwtTokenService;

  public AuthService(
      UserAccountRepository userAccountRepository,
      PasswordEncoder passwordEncoder,
      JwtTokenService jwtTokenService) {
    this.userAccountRepository = userAccountRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtTokenService = jwtTokenService;
  }

  public AuthResponse register(RegisterRequest request) {
    String email = cleanEmail(request.email());
    String displayName = cleanDisplayName(request.displayName(), email);
    validatePassword(request.password());
    if (userAccountRepository.findByEmail(email).isPresent()) {
      throw new IllegalArgumentException("An account already exists for this email");
    }

    Instant now = Instant.now();
    UserAccount user =
        userAccountRepository.save(
            new UserAccount(
                UUID.randomUUID(),
                email,
                displayName,
                passwordEncoder.encode(request.password()),
                "USER",
                now,
                now));
    return authResponse(user);
  }

  public AuthResponse login(LoginRequest request) {
    String email = cleanEmail(request.email());
    UserAccount user =
        userAccountRepository
            .findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
    if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
      throw new IllegalArgumentException("Invalid email or password");
    }
    return authResponse(user);
  }

  public UserProfileResponse profile(UserPrincipal principal) {
    return new UserProfileResponse(
        principal.id(), principal.email(), principal.displayName(), principal.role());
  }

  private AuthResponse authResponse(UserAccount user) {
    return new AuthResponse(jwtTokenService.createToken(user.toPrincipal()), profile(user.toPrincipal()));
  }

  private String cleanEmail(String email) {
    if (email == null || email.isBlank() || !email.contains("@")) {
      throw new IllegalArgumentException("A valid email is required");
    }
    return email.trim().toLowerCase();
  }

  private String cleanDisplayName(String displayName, String email) {
    if (displayName == null || displayName.isBlank()) {
      return email.substring(0, email.indexOf("@"));
    }
    return displayName.trim();
  }

  private void validatePassword(String password) {
    if (password == null || password.length() < 8) {
      throw new IllegalArgumentException("Password must be at least 8 characters");
    }
  }
}
