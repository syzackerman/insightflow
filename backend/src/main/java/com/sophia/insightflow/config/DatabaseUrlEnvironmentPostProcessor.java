package com.sophia.insightflow.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    String databaseUrl = environment.getProperty("DATABASE_URL");
    if (databaseUrl == null || databaseUrl.startsWith("jdbc:")) {
      return;
    }
    if (!databaseUrl.startsWith("postgres://") && !databaseUrl.startsWith("postgresql://")) {
      return;
    }

    URI uri = URI.create(databaseUrl);
    String[] credentials = parseCredentials(uri);
    String jdbcUrl =
        "jdbc:postgresql://"
            + uri.getHost()
            + ":"
            + (uri.getPort() > 0 ? uri.getPort() : 5432)
            + uri.getPath()
            + (uri.getQuery() == null ? "" : "?" + uri.getQuery());

    Map<String, Object> properties = new HashMap<>();
    properties.put("spring.datasource.url", jdbcUrl);
    if (credentials.length > 0 && !credentials[0].isBlank()) {
      properties.put("spring.datasource.username", credentials[0]);
    }
    if (credentials.length > 1 && !credentials[1].isBlank()) {
      properties.put("spring.datasource.password", credentials[1]);
    }
    environment
        .getPropertySources()
        .addFirst(new MapPropertySource("databaseUrlOverrides", properties));
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  private String[] parseCredentials(URI uri) {
    String userInfo = uri.getUserInfo();
    if (userInfo == null || userInfo.isBlank()) {
      return new String[0];
    }
    String[] parts = userInfo.split(":", 2);
    for (int index = 0; index < parts.length; index++) {
      parts[index] = URLDecoder.decode(parts[index], StandardCharsets.UTF_8);
    }
    return parts;
  }
}
