package com.sophia.insightflow.analytics.dto;

import java.util.List;

public record AnalyticsSummaryResponse(
    String generatedAt,
    String source,
    AnalyticsFilter filters,
    FilterOptions filterOptions,
    KpiSummary kpis,
    List<MonthlyRevenueMetric> revenueByMonth,
    List<CategoryMetric> topCategories,
    List<StateMetric> revenueByState,
    List<PaymentMetric> paymentMethodBreakdown,
    List<ReviewScoreMetric> reviewDistribution,
    boolean empty) {}
