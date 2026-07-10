package com.sophia.insightflow.analytics.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record OrderItemFact(
    String orderId,
    String customerUniqueId,
    LocalDate purchaseDate,
    String month,
    String customerState,
    String productCategory,
    List<String> paymentTypes,
    BigDecimal itemRevenue,
    BigDecimal freightValue,
    Integer reviewScore,
    boolean deliveryDelayed) {}
