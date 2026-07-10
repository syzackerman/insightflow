package com.sophia.insightflow.analytics.dto;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public record AnalyticsFilterRequest(
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
    String state,
    String category,
    String paymentType) {

  public AnalyticsFilter toFilter() {
    AnalyticsFilter filter =
        new AnalyticsFilter(
            startDate, endDate, clean(state), clean(category), clean(paymentType));
    filter.validate();
    return filter;
  }

  private static String clean(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
