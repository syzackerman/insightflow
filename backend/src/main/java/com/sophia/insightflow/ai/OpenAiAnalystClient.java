package com.sophia.insightflow.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class OpenAiAnalystClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(OpenAiAnalystClient.class);

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final String provider;
  private final String apiKey;
  private final String model;

  public OpenAiAnalystClient(
      RestClient.Builder restClientBuilder,
      ObjectMapper objectMapper,
      @Value("${insightflow.ai.provider:local}") String provider,
      @Value("${insightflow.ai.api-key:}") String apiKey,
      @Value("${insightflow.ai.base-url:https://api.openai.com/v1}") String baseUrl,
      @Value("${insightflow.ai.model:gpt-5.5}") String model) {
    this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    this.objectMapper = objectMapper;
    this.provider = provider == null ? "local" : provider.trim().toLowerCase();
    this.apiKey = apiKey == null ? "" : apiKey.trim();
    this.model = model == null || model.isBlank() ? "gpt-5.5" : model.trim();
  }

  public LlmCompletion complete(String instructions, String input) {
    if (!isOpenAiEnabled()) {
      return LlmCompletion.local();
    }

    try {
      Map<String, Object> body =
          Map.of(
              "model", model,
              "instructions", instructions,
              "input", input);
      JsonNode response =
          restClient
              .post()
              .uri("/responses")
              .header("Authorization", "Bearer " + apiKey)
              .header("Content-Type", "application/json")
              .body(body)
              .retrieve()
              .body(JsonNode.class);
      String text = extractText(response);
      return text == null || text.isBlank() ? LlmCompletion.local() : LlmCompletion.openAi(text);
    } catch (RestClientException | IllegalArgumentException error) {
      LOGGER.warn("OpenAI analyst request failed; using local fallback: {}", error.getMessage());
      LOGGER.debug("OpenAI analyst fallback cause", error);
      return LlmCompletion.local();
    }
  }

  private boolean isOpenAiEnabled() {
    if ("local".equals(provider)) {
      return false;
    }
    return ("openai".equals(provider) || "auto".equals(provider)) && !apiKey.isBlank();
  }

  private String extractText(JsonNode response) {
    if (response == null) {
      return null;
    }
    JsonNode outputText = response.get("output_text");
    if (outputText != null && outputText.isTextual()) {
      return outputText.asText();
    }
    JsonNode output = response.get("output");
    if (output == null || !output.isArray()) {
      return null;
    }
    StringBuilder builder = new StringBuilder();
    for (JsonNode item : output) {
      JsonNode content = item.get("content");
      if (content == null || !content.isArray()) {
        continue;
      }
      for (JsonNode part : content) {
        JsonNode text = part.get("text");
        if (text != null && text.isTextual()) {
          if (!builder.isEmpty()) {
            builder.append("\n");
          }
          builder.append(text.asText());
        }
      }
    }
    return builder.toString();
  }

  public record LlmCompletion(boolean usedLlm, String text) {
    static LlmCompletion local() {
      return new LlmCompletion(false, "");
    }

    static LlmCompletion openAi(String text) {
      return new LlmCompletion(true, text.trim());
    }
  }
}
