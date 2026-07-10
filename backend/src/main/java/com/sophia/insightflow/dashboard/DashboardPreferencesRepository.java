package com.sophia.insightflow.dashboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DashboardPreferencesRepository {

  private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};
  private static final UUID DEMO_USER_ID =
      UUID.fromString("11111111-1111-4111-8111-111111111111");
  private static final UUID DEMO_DASHBOARD_ID =
      UUID.fromString("22222222-2222-4222-8222-222222222222");

  private final NamedParameterJdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;
  private final Map<UUID, DashboardPreferences> fallbackPreferences = new ConcurrentHashMap<>();
  private volatile boolean databaseAvailable = true;

  public DashboardPreferencesRepository(
      NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
    seedFallbackDemoPreferences();
  }

  public Optional<DashboardPreferences> findByUserId(UUID userId) {
    if (!databaseAvailable) {
      return Optional.ofNullable(fallbackPreferences.get(userId));
    }
    try {
      return jdbcTemplate
          .query(
              """
              SELECT user_id, theme, compact_view, default_dashboard_id, visible_sections, updated_at
              FROM insightflow.dashboard_preferences
              WHERE user_id = :userId
              """,
              Map.of("userId", userId),
              this::mapPreferences)
          .stream()
          .findFirst();
    } catch (DataAccessException error) {
      databaseAvailable = false;
      return Optional.ofNullable(fallbackPreferences.get(userId));
    }
  }

  public DashboardPreferences save(DashboardPreferences preferences) {
    if (databaseAvailable) {
      try {
        jdbcTemplate.update(
            """
            INSERT INTO insightflow.dashboard_preferences
              (user_id, theme, compact_view, default_dashboard_id, visible_sections, updated_at)
            VALUES
              (:userId, :theme, :compactView, :defaultDashboardId, CAST(:visibleSections AS jsonb), :updatedAt)
            ON CONFLICT (user_id) DO UPDATE SET
              theme = EXCLUDED.theme,
              compact_view = EXCLUDED.compact_view,
              default_dashboard_id = EXCLUDED.default_dashboard_id,
              visible_sections = EXCLUDED.visible_sections,
              updated_at = EXCLUDED.updated_at
            """,
            new MapSqlParameterSource()
                .addValue("userId", preferences.userId())
                .addValue("theme", preferences.theme())
                .addValue("compactView", preferences.compactView())
                .addValue("defaultDashboardId", preferences.defaultDashboardId())
                .addValue("visibleSections", writeSections(preferences.visibleSections()))
                .addValue("updatedAt", preferences.updatedAt()));
        return preferences;
      } catch (DataAccessException error) {
        databaseAvailable = false;
      }
    }
    fallbackPreferences.put(preferences.userId(), preferences);
    return preferences;
  }

  private DashboardPreferences mapPreferences(ResultSet resultSet, int rowNumber)
      throws SQLException {
    return new DashboardPreferences(
        resultSet.getObject("user_id", UUID.class),
        resultSet.getString("theme"),
        resultSet.getBoolean("compact_view"),
        resultSet.getObject("default_dashboard_id", UUID.class),
        readSections(resultSet.getString("visible_sections")),
        resultSet.getTimestamp("updated_at").toInstant());
  }

  private String writeSections(List<String> sections) {
    try {
      return objectMapper.writeValueAsString(sections);
    } catch (JsonProcessingException error) {
      return "[]";
    }
  }

  private List<String> readSections(String sections) {
    try {
      return objectMapper.readValue(sections, STRING_LIST);
    } catch (Exception error) {
      return List.of();
    }
  }

  private void seedFallbackDemoPreferences() {
    fallbackPreferences.put(
        DEMO_USER_ID,
        new DashboardPreferences(
            DEMO_USER_ID,
            "light",
            false,
            DEMO_DASHBOARD_ID,
            List.of("kpis", "ai", "trend", "categories", "states", "reviews", "payments"),
            Instant.parse("2026-01-01T00:00:00Z")));
  }
}
