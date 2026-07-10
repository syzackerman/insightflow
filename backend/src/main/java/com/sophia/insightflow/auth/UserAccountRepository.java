package com.sophia.insightflow.auth;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserAccountRepository {

  private static final UUID DEMO_USER_ID =
      UUID.fromString("11111111-1111-4111-8111-111111111111");
  private static final String DEMO_EMAIL = "demo@example.com";
  private static final String DEMO_PASSWORD_HASH =
      "$2a$10$o7gRsuZRRgY.qouvzNhWSONPCd2iki5Y3jyoGqXNtvXFLTBBSFsq.";

  private final NamedParameterJdbcTemplate jdbcTemplate;
  private final Map<String, UserAccount> fallbackByEmail = new ConcurrentHashMap<>();
  private final Map<UUID, UserAccount> fallbackById = new ConcurrentHashMap<>();
  private volatile boolean databaseAvailable = true;

  public UserAccountRepository(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
    seedFallbackDemoUser();
  }

  public Optional<UserAccount> findByEmail(String email) {
    String normalized = normalizeEmail(email);
    if (!databaseAvailable) {
      return Optional.ofNullable(fallbackByEmail.get(normalized));
    }
    try {
      return jdbcTemplate
          .query(
              """
              SELECT id, email, display_name, password_hash, role, created_at, updated_at
              FROM insightflow.app_users
              WHERE email = :email
              """,
              Map.of("email", normalized),
              this::mapUser)
          .stream()
          .findFirst();
    } catch (DataAccessException error) {
      databaseAvailable = false;
      return Optional.ofNullable(fallbackByEmail.get(normalized));
    }
  }

  public Optional<UserAccount> findById(UUID id) {
    if (!databaseAvailable) {
      return Optional.ofNullable(fallbackById.get(id));
    }
    try {
      return jdbcTemplate
          .query(
              """
              SELECT id, email, display_name, password_hash, role, created_at, updated_at
              FROM insightflow.app_users
              WHERE id = :id
              """,
              Map.of("id", id),
              this::mapUser)
          .stream()
          .findFirst();
    } catch (DataAccessException error) {
      databaseAvailable = false;
      return Optional.ofNullable(fallbackById.get(id));
    }
  }

  public UserAccount save(UserAccount user) {
    UserAccount normalized =
        new UserAccount(
            user.id(),
            normalizeEmail(user.email()),
            user.displayName(),
            user.passwordHash(),
            user.role(),
            user.createdAt(),
            user.updatedAt());
    if (databaseAvailable) {
      try {
        jdbcTemplate.update(
            """
            INSERT INTO insightflow.app_users
              (id, email, display_name, password_hash, role, created_at, updated_at)
            VALUES
              (:id, :email, :displayName, :passwordHash, :role, :createdAt, :updatedAt)
            """,
            new MapSqlParameterSource()
                .addValue("id", normalized.id())
                .addValue("email", normalized.email())
                .addValue("displayName", normalized.displayName())
                .addValue("passwordHash", normalized.passwordHash())
                .addValue("role", normalized.role())
                .addValue("createdAt", normalized.createdAt())
                .addValue("updatedAt", normalized.updatedAt()));
        return normalized;
      } catch (DataAccessException error) {
        databaseAvailable = false;
      }
    }
    fallbackByEmail.put(normalized.email(), normalized);
    fallbackById.put(normalized.id(), normalized);
    return normalized;
  }

  private UserAccount mapUser(ResultSet resultSet, int rowNumber) throws SQLException {
    return new UserAccount(
        resultSet.getObject("id", UUID.class),
        resultSet.getString("email"),
        resultSet.getString("display_name"),
        resultSet.getString("password_hash"),
        resultSet.getString("role"),
        resultSet.getTimestamp("created_at").toInstant(),
        resultSet.getTimestamp("updated_at").toInstant());
  }

  private String normalizeEmail(String email) {
    return email == null ? "" : email.trim().toLowerCase();
  }

  private void seedFallbackDemoUser() {
    Instant now = Instant.parse("2026-01-01T00:00:00Z");
    UserAccount demoUser =
        new UserAccount(
            DEMO_USER_ID,
            DEMO_EMAIL,
            "Demo User",
            DEMO_PASSWORD_HASH,
            "USER",
            now,
            now);
    fallbackByEmail.put(DEMO_EMAIL, demoUser);
    fallbackById.put(DEMO_USER_ID, demoUser);
  }
}
