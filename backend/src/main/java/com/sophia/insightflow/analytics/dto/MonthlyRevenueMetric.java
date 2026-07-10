package com.sophia.insightflow.analytics.dto;

import java.math.BigDecimal;

public record MonthlyRevenueMetric(String month, BigDecimal revenue, long orders) {}
