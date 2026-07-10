package com.sophia.insightflow.analytics.dto;

import java.time.LocalDate;

public record AnalyticsFilter(
    LocalDate startDate, LocalDate endDate, String state, String category, String paymentType) {

  public void validate() {
    if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
      throw new IllegalArgumentException("startDate must be on or before endDate");
    }
  }
}
