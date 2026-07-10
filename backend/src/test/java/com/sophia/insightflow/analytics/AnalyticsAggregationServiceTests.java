package com.sophia.insightflow.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.sophia.insightflow.analytics.data.AnalyticsDataset;
import com.sophia.insightflow.analytics.data.OrderItemFact;
import com.sophia.insightflow.analytics.data.PaymentFact;
import com.sophia.insightflow.analytics.dto.AnalyticsFilter;
import com.sophia.insightflow.analytics.dto.AnalyticsSummaryResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnalyticsAggregationServiceTests {

  private final AnalyticsAggregationService service =
      new AnalyticsAggregationService(() -> sampleDataset());

  @Test
  void calculatesCoreBusinessMetrics() {
    AnalyticsSummaryResponse summary =
        service.summarize(new AnalyticsFilter(null, null, null, null, null));

    assertThat(summary.kpis().totalRevenue()).isEqualByComparingTo("650.00");
    assertThat(summary.kpis().totalOrders()).isEqualTo(3);
    assertThat(summary.kpis().averageOrderValue()).isEqualByComparingTo("216.67");
    assertThat(summary.kpis().repeatCustomerRate()).isEqualTo(50.0);
    assertThat(summary.kpis().averageReviewScore()).isEqualTo(4.0);
    assertThat(summary.kpis().deliveryDelayRate()).isEqualTo(33.3);
    assertThat(summary.topCategories().getFirst().category()).isEqualTo("Cat A");
    assertThat(summary.topCategories().getFirst().revenue()).isEqualByComparingTo("600.00");
  }

  @Test
  void filtersByStateCategoryPaymentAndDate() {
    AnalyticsSummaryResponse summary =
        service.summarize(
            new AnalyticsFilter(
                LocalDate.parse("2018-01-01"),
                LocalDate.parse("2018-01-31"),
                "SP",
                "Cat A",
                "Credit Card"));

    assertThat(summary.empty()).isFalse();
    assertThat(summary.kpis().totalRevenue()).isEqualByComparingTo("100.00");
    assertThat(summary.kpis().totalOrders()).isEqualTo(1);
    assertThat(summary.revenueByMonth()).hasSize(1);
    assertThat(summary.paymentMethodBreakdown().getFirst().paymentType()).isEqualTo("Credit Card");
  }

  @Test
  void returnsEmptySummaryWhenNoFactsMatch() {
    AnalyticsSummaryResponse summary =
        service.summarize(new AnalyticsFilter(null, null, "ZZ", null, null));

    assertThat(summary.empty()).isTrue();
    assertThat(summary.kpis().totalRevenue()).isEqualByComparingTo("0.00");
    assertThat(summary.kpis().totalOrders()).isZero();
    assertThat(summary.revenueByMonth()).isEmpty();
    assertThat(summary.paymentMethodBreakdown()).isEmpty();
  }

  private AnalyticsDataset sampleDataset() {
    return new AnalyticsDataset(
        "2026-07-09T00:00:00Z",
        "test dataset",
        List.of(
            item("o1", "cust-a", "2018-01-10", "SP", "Cat A", "Credit Card", "100.00", 5, false),
            item("o1", "cust-a", "2018-01-10", "SP", "Cat B", "Credit Card", "50.00", 5, false),
            item("o2", "cust-a", "2018-02-10", "SP", "Cat A", "Boleto", "200.00", 3, true),
            item("o3", "cust-b", "2018-02-15", "RJ", "Cat A", "Credit Card", "300.00", 4, false)),
        List.of(
            new PaymentFact("o1", "Credit Card", new BigDecimal("150.00")),
            new PaymentFact("o2", "Boleto", new BigDecimal("220.00")),
            new PaymentFact("o3", "Credit Card", new BigDecimal("330.00"))));
  }

  private OrderItemFact item(
      String orderId,
      String customerId,
      String purchaseDate,
      String state,
      String category,
      String paymentType,
      String revenue,
      Integer reviewScore,
      boolean delayed) {
    LocalDate date = LocalDate.parse(purchaseDate);
    return new OrderItemFact(
        orderId,
        customerId,
        date,
        purchaseDate.substring(0, 7),
        state,
        category,
        List.of(paymentType),
        new BigDecimal(revenue),
        BigDecimal.ZERO,
        reviewScore,
        delayed);
  }
}
