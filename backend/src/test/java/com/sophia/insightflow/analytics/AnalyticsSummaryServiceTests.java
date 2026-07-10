package com.sophia.insightflow.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sophia.insightflow.analytics.dto.AnalyticsFilter;
import com.sophia.insightflow.analytics.dto.AnalyticsSummaryResponse;
import com.sophia.insightflow.analytics.dto.FilterOptions;
import com.sophia.insightflow.analytics.dto.KpiSummary;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

class AnalyticsSummaryServiceTests {

  private static final AnalyticsFilter EMPTY_FILTER =
      new AnalyticsFilter(null, null, null, null, null);

  @Test
  void autoModeFallsBackToJsonWhenPostgresUnavailable() {
    PostgresAnalyticsRepository postgresRepository = mock(PostgresAnalyticsRepository.class);
    AnalyticsAggregationService jsonService = mock(AnalyticsAggregationService.class);
    AnalyticsSummaryResponse fallbackResponse = response("json", 10);

    when(postgresRepository.summarize(any()))
        .thenThrow(new DataAccessResourceFailureException("database unavailable"));
    when(jsonService.summarize(EMPTY_FILTER)).thenReturn(fallbackResponse);

    AnalyticsSummaryService service =
        new AnalyticsSummaryService(postgresRepository, jsonService, "auto");

    assertThat(service.summarize(EMPTY_FILTER)).isSameAs(fallbackResponse);
  }

  @Test
  void postgresModeDoesNotHideDatabaseErrors() {
    PostgresAnalyticsRepository postgresRepository = mock(PostgresAnalyticsRepository.class);
    AnalyticsAggregationService jsonService = mock(AnalyticsAggregationService.class);

    when(postgresRepository.summarize(any()))
        .thenThrow(new DataAccessResourceFailureException("database unavailable"));

    AnalyticsSummaryService service =
        new AnalyticsSummaryService(postgresRepository, jsonService, "postgres");

    assertThatThrownBy(() -> service.summarize(EMPTY_FILTER))
        .isInstanceOf(DataAccessResourceFailureException.class);
  }

  @Test
  void jsonModeUsesFallbackSourceDirectly() {
    PostgresAnalyticsRepository postgresRepository = mock(PostgresAnalyticsRepository.class);
    AnalyticsAggregationService jsonService = mock(AnalyticsAggregationService.class);
    AnalyticsSummaryResponse fallbackResponse = response("json", 10);

    when(jsonService.summarize(EMPTY_FILTER)).thenReturn(fallbackResponse);

    AnalyticsSummaryService service =
        new AnalyticsSummaryService(postgresRepository, jsonService, "json");

    assertThat(service.summarize(EMPTY_FILTER)).isSameAs(fallbackResponse);
  }

  private AnalyticsSummaryResponse response(String source, long orders) {
    return new AnalyticsSummaryResponse(
        "2026-07-09T00:00:00Z",
        source,
        EMPTY_FILTER,
        new FilterOptions(null, null, List.of(), List.of(), List.of()),
        new KpiSummary(BigDecimal.TEN, orders, BigDecimal.ONE, 0, null, 0),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        orders == 0);
  }
}
