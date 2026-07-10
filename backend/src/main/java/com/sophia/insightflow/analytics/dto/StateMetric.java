package com.sophia.insightflow.analytics.dto;

import java.math.BigDecimal;

public record StateMetric(String state, BigDecimal revenue, long orders) {}
