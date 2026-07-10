package com.sophia.insightflow.dashboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sophia.insightflow.analytics.dto.AnalyticsFilter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
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
public class SavedDashboardRepository {

  private static final UUID DEMO_USER_ID =
      UUID.fromString("11111111-1111-4111-8111-111111111111");
  private static final UUID DEMO_DASHBOARD_ID =
      UUID.fromString("22222222-2222-4222-8222-222222222222");

  private final NamedParameterJdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;
  private final Map<UUID, SavedDashboard> fallbackDashboards = new ConcurrentHashMap<>();
  private volatile boolean databaseAvailable = true;

  public SavedDashboardRepository(
      NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
    seedFallbackDemoDashboard();
  }

  public List<SavedDashboard> findAllByUserId(UUID userId) {
    if (!databaseAvailable) {
      return fallbackDashboards.values().stream()
          .filter(dashboard -> dashboard.userId().equals(userId))
          .sorted(Comparator.comparing(SavedDashboard::updatedAt).reversed())
          .toList();
    }
    try {
      return jdbcTemplate.query(
          """
          SELECT id, user_id, name, description, filters, created_at, updated_at
          FROM insightflow.saved_dashboards
          WHERE user_id = :userId
          ORDER BY updated_at DESC
          """,
          Map.of("userId", userId),
          this::mapDashboard);
    } catch (DataAccessException error) {
      databaseAvailable = false;
      return findAllByUserId(userId);
    }
  }

  public Optional<SavedDashboard> findByIdAndUserId(UUID id, UUID userId) {
    if (!databaseAvailable) {
      SavedDashboard dashboard = fallbackDashboards.get(id);
      return dashboard != null && dashboard.userId().equals(userId)
          ? Optional.of(dashboard)
          : Optional.empty();
    }
    try {
      return jdbcTemplate
          .query(
              """
              SELECT id, user_id, name, description, filters, created_at, updated_at
              FROM insightflow.saved_dashboards
              WHERE id = :id AND user_id = :userId
              """,
              Map.of("id", id, "userId", userId),
              this::mapDashboard)
          .stream()
          .findFirst();
    } catch (DataAccessException error) {
      databaseAvailable = false;
      return findByIdAndUserId(id, userId);
    }
  }

  public SavedDashboard save(SavedDashboard dashboard) {
    if (databaseAvailable) {
      try {
        jdbcTemplate.update(
            """
            INSERT INTO insightflow.saved_dashboards
              (id, user_id, name, description, filters, created_at, updated_at)
            VALUES
              (:id, :userId, :name, :description, CAST(:filters AS jsonb), :createdAt, :updatedAt)
            ON CONFLICT (id) DO UPDATE SET
              name = EXCLUDED.name,
              description = EXCLUDED.description,
              filters = EXCLUDED.filters,
              updated_at = EXCLUDED.updated_at
            """,
            new MapSqlParameterSource()
                .addValue("id", dashboard.id())
                .addValue("userId", dashboard.userId())
                .addValue("name", dashboard.name())
                .addValue("description", dashboard.description())
                .addValue("filters", writeFilters(dashboard.filters()))
                .addValue("createdAt", dashboard.createdAt())
                .addValue("updatedAt", dashboard.updatedAt()));
        return dashboard;
      } catch (DataAccessException error) {
        databaseAvailable = false;
      }
    }
    fallbackDashboards.put(dashboard.id(), dashboard);
    return dashboard;
  }

  public void delete(UUID id, UUID userId) {
    if (databaseAvailable) {
      try {
        jdbcTemplate.update(
            "DELETE FROM insightflow.saved_dashboards WHERE id = :id AND user_id = :userId",
            Map.of("id", id, "userId", userId));
        return;
      } catch (DataAccessException error) {
        databaseAvailable = false;
      }
    }
    fallbackDashboards.computeIfPresent(
        id, (dashboardId, dashboard) -> dashboard.userId().equals(userId) ? null : dashboard);
  }

  private SavedDashboard mapDashboard(ResultSet resultSet, int rowNumber) throws SQLException {
    return new SavedDashboard(
        resultSet.getObject("id", UUID.class),
        resultSet.getObject("user_id", UUID.class),
        resultSet.getString("name"),
        resultSet.getString("description"),
        readFilters(resultSet.getString("filters")),
        resultSet.getTimestamp("created_at").toInstant(),
        resultSet.getTimestamp("updated_at").toInstant());
  }

  private String writeFilters(AnalyticsFilter filters) {
    try {
      return objectMapper.writeValueAsString(filters);
    } catch (JsonProcessingException error) {
      return "{}";
    }
  }

  private AnalyticsFilter readFilters(String filters) {
    try {
      return objectMapper.readValue(filters, AnalyticsFilter.class);
    } catch (Exception error) {
      return new AnalyticsFilter(null, null, null, null, null);
    }
  }

  private void seedFallbackDemoDashboard() {
    Instant now = Instant.parse("2026-01-01T00:00:00Z");
    fallbackDashboards.put(
        DEMO_DASHBOARD_ID,
        new SavedDashboard(
            DEMO_DASHBOARD_ID,
            DEMO_USER_ID,
            "SP credit card performance",
            "Demo saved view for portfolio walkthroughs",
            new AnalyticsFilter(null, null, "SP", null, "Credit Card"),
            now,
            now));
  }
}
