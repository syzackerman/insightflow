package com.sophia.insightflow.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.sophia.insightflow.analytics.dto.AnalyticsFilter;
import com.sophia.insightflow.analytics.dto.KpiSummary;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class PostgresAnalyticsRepositoryTests {

  @Test
  void summarizeRunsPostgresQueriesWithFilterParameters() {
    NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
    JdbcOperations jdbcOperations = mock(JdbcOperations.class);
    when(jdbcTemplate.getJdbcOperations()).thenReturn(jdbcOperations);
    PostgresAnalyticsRepository repository = new PostgresAnalyticsRepository(jdbcTemplate);

    when(jdbcTemplate.queryForObject(
            anyString(), any(MapSqlParameterSource.class), anyKpiMapper()))
        .thenReturn(new KpiSummary(new BigDecimal("100.00"), 1, new BigDecimal("100.00"), 0, 5.0, 0));
    when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), anyMonthlyMapper()))
        .thenReturn(List.of());
    when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), anyCategoryMapper()))
        .thenReturn(List.of());
    when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), anyStateMapper()))
        .thenReturn(List.of());
    when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), anyPaymentMapper()))
        .thenReturn(List.of());
    when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), anyReviewMapper()))
        .thenReturn(List.of());
    when(jdbcTemplate.queryForObject(anyString(), eq(Map.of()), anyDateMapper())).thenReturn(null);
    when(jdbcTemplate.queryForList(anyString(), eq(Map.of()), eq(String.class))).thenReturn(List.of());

    AnalyticsFilter filter =
        new AnalyticsFilter(
            LocalDate.parse("2018-01-01"),
            LocalDate.parse("2018-08-31"),
            "SP",
            "Health Beauty",
            "Credit Card");

    repository.summarize(filter);

    ArgumentCaptor<MapSqlParameterSource> paramsCaptor =
        ArgumentCaptor.forClass(MapSqlParameterSource.class);
    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

    org.mockito.Mockito.verify(jdbcTemplate)
        .queryForObject(sqlCaptor.capture(), paramsCaptor.capture(), anyKpiMapper());

    ArgumentCaptor<String> createSqlCaptor = ArgumentCaptor.forClass(String.class);
    org.mockito.Mockito.verify(jdbcTemplate).update(createSqlCaptor.capture(), paramsCaptor.capture());

    assertThat(createSqlCaptor.getValue())
        .contains("CREATE TEMP TABLE filtered_analytics_items", "insightflow.orders");
    assertThat(createSqlCaptor.getValue()).contains("order_payments");
    assertThat(createSqlCaptor.getValue())
        .contains("SELECT order_id, max(review_score) AS review_score");
    assertThat(createSqlCaptor.getValue())
        .contains(
            "o.order_purchase_timestamp >= :startDate",
            "o.order_purchase_timestamp < :endExclusiveDate",
            "c.customer_state = :stateCode",
            "= :categoryKey",
            "op.payment_type = :paymentTypeKey");
    assertThat(createSqlCaptor.getValue())
        .doesNotContainPattern(":[A-Za-z][A-Za-z0-9]*\\s+IS\\s+NULL");

    assertThat(sqlCaptor.getValue()).contains("filtered_analytics_orders");
    assertThat(sqlCaptor.getValue()).doesNotContain("insightflow.orders");
    assertThat(paramsCaptor.getValue().getValue("startDate")).isEqualTo(LocalDate.parse("2018-01-01"));
    assertThat(paramsCaptor.getValue().getValue("endExclusiveDate"))
        .isEqualTo(LocalDate.parse("2018-09-01"));
    assertThat(paramsCaptor.getValue().getValue("stateCode")).isEqualTo("SP");
    assertThat(paramsCaptor.getValue().getValue("categoryKey")).isEqualTo("health beauty");
    assertThat(paramsCaptor.getValue().getValue("paymentTypeKey")).isEqualTo("credit_card");

    ArgumentCaptor<String> querySqlCaptor = ArgumentCaptor.forClass(String.class);
    org.mockito.Mockito.verify(jdbcTemplate)
        .queryForObject(sqlCaptor.capture(), any(MapSqlParameterSource.class), anyKpiMapper());
    org.mockito.Mockito.verify(jdbcTemplate, times(5))
        .query(querySqlCaptor.capture(), any(MapSqlParameterSource.class), any(RowMapper.class));
    String paymentSql =
        querySqlCaptor.getAllValues().stream()
            .filter(sql -> sql.contains("payment_totals"))
            .findFirst()
            .orElseThrow();
    assertThat(paymentSql)
        .contains("filtered_analytics_orders", "op.payment_type = :paymentTypeKey");
    assertThat(paymentSql).doesNotContain("WITH filtered_items AS");
    assertThat(paymentSql).doesNotContainPattern(":[A-Za-z][A-Za-z0-9]*\\s+IS\\s+NULL");
  }

  @Test
  void summarizeOmitsNullableFilterPredicatesWhenFiltersAreMissing() {
    NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
    JdbcOperations jdbcOperations = mock(JdbcOperations.class);
    when(jdbcTemplate.getJdbcOperations()).thenReturn(jdbcOperations);
    PostgresAnalyticsRepository repository = new PostgresAnalyticsRepository(jdbcTemplate);

    when(jdbcTemplate.queryForObject(
            anyString(), any(MapSqlParameterSource.class), anyKpiMapper()))
        .thenReturn(new KpiSummary(new BigDecimal("100.00"), 1, new BigDecimal("100.00"), 0, 5.0, 0));
    when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), anyMonthlyMapper()))
        .thenReturn(List.of());
    when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), anyCategoryMapper()))
        .thenReturn(List.of());
    when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), anyStateMapper()))
        .thenReturn(List.of());
    when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), anyPaymentMapper()))
        .thenReturn(List.of());
    when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), anyReviewMapper()))
        .thenReturn(List.of());
    when(jdbcTemplate.queryForObject(anyString(), eq(Map.of()), anyDateMapper())).thenReturn(null);
    when(jdbcTemplate.queryForList(anyString(), eq(Map.of()), eq(String.class))).thenReturn(List.of());

    repository.summarize(new AnalyticsFilter(null, null, null, null, null));

    ArgumentCaptor<String> createSqlCaptor = ArgumentCaptor.forClass(String.class);
    org.mockito.Mockito.verify(jdbcTemplate).update(createSqlCaptor.capture(), any(MapSqlParameterSource.class));

    assertThat(createSqlCaptor.getValue()).contains("CREATE TEMP TABLE filtered_analytics_items");
    assertThat(createSqlCaptor.getValue())
        .doesNotContain(
            ":startDate",
            ":endExclusiveDate",
            ":stateCode",
            ":categoryKey",
            ":paymentTypeKey");
  }

  @SuppressWarnings("unchecked")
  private RowMapper<KpiSummary> anyKpiMapper() {
    return (RowMapper<KpiSummary>) any(RowMapper.class);
  }

  @SuppressWarnings("unchecked")
  private RowMapper<com.sophia.insightflow.analytics.dto.MonthlyRevenueMetric> anyMonthlyMapper() {
    return (RowMapper<com.sophia.insightflow.analytics.dto.MonthlyRevenueMetric>) any(RowMapper.class);
  }

  @SuppressWarnings("unchecked")
  private RowMapper<com.sophia.insightflow.analytics.dto.CategoryMetric> anyCategoryMapper() {
    return (RowMapper<com.sophia.insightflow.analytics.dto.CategoryMetric>) any(RowMapper.class);
  }

  @SuppressWarnings("unchecked")
  private RowMapper<com.sophia.insightflow.analytics.dto.StateMetric> anyStateMapper() {
    return (RowMapper<com.sophia.insightflow.analytics.dto.StateMetric>) any(RowMapper.class);
  }

  @SuppressWarnings("unchecked")
  private RowMapper<com.sophia.insightflow.analytics.dto.PaymentMetric> anyPaymentMapper() {
    return (RowMapper<com.sophia.insightflow.analytics.dto.PaymentMetric>) any(RowMapper.class);
  }

  @SuppressWarnings("unchecked")
  private RowMapper<com.sophia.insightflow.analytics.dto.ReviewScoreMetric> anyReviewMapper() {
    return (RowMapper<com.sophia.insightflow.analytics.dto.ReviewScoreMetric>) any(RowMapper.class);
  }

  @SuppressWarnings("unchecked")
  private RowMapper<LocalDate> anyDateMapper() {
    return (RowMapper<LocalDate>) any(RowMapper.class);
  }
}
