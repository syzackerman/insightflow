package com.sophia.insightflow.analytics.dto;

import java.math.BigDecimal;

public record KpiSummary(
    BigDecimal totalRevenue,
    long totalOrders,
    BigDecimal averageOrderValue,
    double repeatCustomerRate,
    Double averageReviewScore,
    double deliveryDelayRate) {}
