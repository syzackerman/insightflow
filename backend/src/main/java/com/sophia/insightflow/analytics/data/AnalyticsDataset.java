package com.sophia.insightflow.analytics.data;

import java.util.List;

public record AnalyticsDataset(
    String generatedAt, String source, List<OrderItemFact> items, List<PaymentFact> payments) {}
