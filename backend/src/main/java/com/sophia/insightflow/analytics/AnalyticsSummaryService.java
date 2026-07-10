package com.sophia.insightflow.analytics;

import com.sophia.insightflow.analytics.dto.AnalyticsFilter;
import com.sophia.insightflow.analytics.dto.AnalyticsSummaryResponse;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsSummaryService {

  private static final Logger LOGGER = LoggerFactory.getLogger(AnalyticsSummaryService.class);

  private final PostgresAnalyticsRepository postgresAnalyticsRepository;
  private final AnalyticsAggregationService jsonAggregationService;
  private final AnalyticsMode analyticsMode;

  public AnalyticsSummaryService(
      PostgresAnalyticsRepository postgresAnalyticsRepository,
      AnalyticsAggregationService jsonAggregationService,
      @Value("${insightflow.analytics.mode:auto}") String analyticsMode) {
    this.postgresAnalyticsRepository = postgresAnalyticsRepository;
    this.jsonAggregationService = jsonAggregationService;
    this.analyticsMode = parseMode(analyticsMode);
  }

  public AnalyticsSummaryResponse summarize(AnalyticsFilter filter) {
    if (analyticsMode == AnalyticsMode.JSON) {
      return jsonAggregationService.summarize(filter);
    }

    try {
      return postgresAnalyticsRepository.summarize(filter);
    } catch (RuntimeException exception) {
      if (analyticsMode == AnalyticsMode.POSTGRES) {
        throw exception;
      }
      LOGGER.warn("PostgreSQL analytics unavailable; using generated JSON fallback: {}", exception.getMessage());
      LOGGER.debug("PostgreSQL analytics fallback cause", exception);
      return jsonAggregationService.summarize(filter);
    }
  }

  private AnalyticsMode parseMode(String value) {
    return AnalyticsMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
  }
}
