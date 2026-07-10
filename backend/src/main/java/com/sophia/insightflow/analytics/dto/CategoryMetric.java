package com.sophia.insightflow.analytics.dto;

import java.math.BigDecimal;

public record CategoryMetric(
    String category, BigDecimal revenue, long orders, long items, Double averageReviewScore) {}
