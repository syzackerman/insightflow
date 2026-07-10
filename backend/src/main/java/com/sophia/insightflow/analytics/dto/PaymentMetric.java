package com.sophia.insightflow.analytics.dto;

import java.math.BigDecimal;

public record PaymentMetric(String paymentType, BigDecimal value, double share) {}
