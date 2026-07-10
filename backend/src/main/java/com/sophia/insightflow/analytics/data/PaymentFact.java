package com.sophia.insightflow.analytics.data;

import java.math.BigDecimal;

public record PaymentFact(String orderId, String paymentType, BigDecimal paymentValue) {}
