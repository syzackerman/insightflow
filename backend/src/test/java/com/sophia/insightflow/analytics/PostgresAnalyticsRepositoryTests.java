package com.sophia.insightflow.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class PostgresAnalyticsRepositoryTests {

  @Test
  void summarizeRunsPostgresQueriesWithFilterParameters() {
    NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
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

    assertThat(sqlCaptor.getValue()).contains("WITH filtered_items AS", "insightflow.orders");
    assertThat(sqlCaptor.getValue()).contains("order_payments");
    assertThat(sqlCaptor.getValue()).contains("SELECT order_id, max(review_score) AS review_score");
    assertThat(paramsCaptor.getValue().getValue("startDate")).isEqualTo(LocalDate.parse("2018-01-01"));
    assertThat(paramsCaptor.getValue().getValue("endDate")).isEqualTo(LocalDate.parse("2018-08-31"));
    assertThat(paramsCaptor.getValue().getValue("state")).isEqualTo("SP");
    assertThat(paramsCaptor.getValue().getValue("category")).isEqualTo("Health Beauty");
    assertThat(paramsCaptor.getValue().getValue("paymentType")).isEqualTo("Credit Card");
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
