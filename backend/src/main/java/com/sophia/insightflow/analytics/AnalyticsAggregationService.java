package com.sophia.insightflow.analytics;

import com.sophia.insightflow.analytics.data.AnalyticsDataRepository;
import com.sophia.insightflow.analytics.data.AnalyticsDataset;
import com.sophia.insightflow.analytics.data.OrderItemFact;
import com.sophia.insightflow.analytics.data.PaymentFact;
import com.sophia.insightflow.analytics.dto.AnalyticsFilter;
import com.sophia.insightflow.analytics.dto.AnalyticsSummaryResponse;
import com.sophia.insightflow.analytics.dto.CategoryMetric;
import com.sophia.insightflow.analytics.dto.FilterOptions;
import com.sophia.insightflow.analytics.dto.KpiSummary;
import com.sophia.insightflow.analytics.dto.MonthlyRevenueMetric;
import com.sophia.insightflow.analytics.dto.PaymentMetric;
import com.sophia.insightflow.analytics.dto.ReviewScoreMetric;
import com.sophia.insightflow.analytics.dto.StateMetric;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsAggregationService {

  private static final int MONEY_SCALE = 2;

  private final AnalyticsDataRepository analyticsDataRepository;

  public AnalyticsAggregationService(AnalyticsDataRepository analyticsDataRepository) {
    this.analyticsDataRepository = analyticsDataRepository;
  }

  public AnalyticsSummaryResponse summarize(AnalyticsFilter filter) {
    AnalyticsDataset dataset = analyticsDataRepository.getDataset();
    List<OrderItemFact> filteredItems =
        dataset.items().stream().filter(item -> matches(item, filter)).toList();

    Map<String, OrderAccumulator> orders = aggregateOrders(filteredItems);
    Set<String> orderIds = orders.keySet();
    BigDecimal totalRevenue = sum(filteredItems.stream().map(OrderItemFact::itemRevenue).toList());

    KpiSummary kpis = buildKpis(totalRevenue, orders);
    List<MonthlyRevenueMetric> revenueByMonth = buildMonthlyRevenue(filteredItems);
    List<CategoryMetric> topCategories = buildTopCategories(filteredItems);
    List<StateMetric> revenueByState = buildRevenueByState(filteredItems);
    List<PaymentMetric> paymentMethodBreakdown =
        buildPaymentBreakdown(dataset.payments(), orderIds, filter.paymentType());
    List<ReviewScoreMetric> reviewDistribution = buildReviewDistribution(orders);

    return new AnalyticsSummaryResponse(
        dataset.generatedAt(),
        dataset.source(),
        filter,
        buildFilterOptions(dataset),
        kpis,
        revenueByMonth,
        topCategories,
        revenueByState,
        paymentMethodBreakdown,
        reviewDistribution,
        filteredItems.isEmpty());
  }

  private boolean matches(OrderItemFact item, AnalyticsFilter filter) {
    if (filter.startDate() != null && item.purchaseDate().isBefore(filter.startDate())) {
      return false;
    }
    if (filter.endDate() != null && item.purchaseDate().isAfter(filter.endDate())) {
      return false;
    }
    if (filter.state() != null && !same(item.customerState(), filter.state())) {
      return false;
    }
    if (filter.category() != null && !same(item.productCategory(), filter.category())) {
      return false;
    }
    return filter.paymentType() == null
        || item.paymentTypes().stream().anyMatch(type -> same(type, filter.paymentType()));
  }

  private Map<String, OrderAccumulator> aggregateOrders(List<OrderItemFact> items) {
    Map<String, OrderAccumulator> orders = new LinkedHashMap<>();
    for (OrderItemFact item : items) {
      orders.computeIfAbsent(item.orderId(), id -> new OrderAccumulator(item)).add(item);
    }
    return orders;
  }

  private KpiSummary buildKpis(BigDecimal totalRevenue, Map<String, OrderAccumulator> orders) {
    long totalOrders = orders.size();
    BigDecimal averageOrderValue =
        totalOrders == 0
            ? BigDecimal.ZERO.setScale(MONEY_SCALE)
            : totalRevenue.divide(BigDecimal.valueOf(totalOrders), MONEY_SCALE, RoundingMode.HALF_UP);

    Map<String, Long> customerOrderCounts =
        orders.values().stream()
            .collect(Collectors.groupingBy(OrderAccumulator::customerUniqueId, Collectors.counting()));

    long repeatCustomers =
        customerOrderCounts.values().stream().filter(orderCount -> orderCount > 1).count();
    double repeatCustomerRate =
        customerOrderCounts.isEmpty()
            ? 0
            : roundPercent((double) repeatCustomers / customerOrderCounts.size());

    List<Integer> reviewScores =
        orders.values().stream()
            .map(OrderAccumulator::reviewScore)
            .filter(Objects::nonNull)
            .toList();
    Double averageReviewScore =
        reviewScores.isEmpty()
            ? null
            : roundDouble(reviewScores.stream().mapToInt(Integer::intValue).average().orElse(0), 2);

    long delayedOrders = orders.values().stream().filter(OrderAccumulator::deliveryDelayed).count();
    double deliveryDelayRate =
        totalOrders == 0 ? 0 : roundPercent((double) delayedOrders / totalOrders);

    return new KpiSummary(
        totalRevenue.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
        totalOrders,
        averageOrderValue,
        repeatCustomerRate,
        averageReviewScore,
        deliveryDelayRate);
  }

  private List<MonthlyRevenueMetric> buildMonthlyRevenue(List<OrderItemFact> items) {
    Map<String, SegmentAccumulator> monthly = new TreeMap<>();
    for (OrderItemFact item : items) {
      monthly.computeIfAbsent(item.month(), ignored -> new SegmentAccumulator()).add(item);
    }
    return monthly.entrySet().stream()
        .map(
            entry ->
                new MonthlyRevenueMetric(
                    entry.getKey(), entry.getValue().revenue(), entry.getValue().orderCount()))
        .toList();
  }

  private List<CategoryMetric> buildTopCategories(List<OrderItemFact> items) {
    Map<String, SegmentAccumulator> categories = new HashMap<>();
    for (OrderItemFact item : items) {
      categories
          .computeIfAbsent(item.productCategory(), ignored -> new SegmentAccumulator())
          .add(item);
    }
    return categories.entrySet().stream()
        .sorted(metricComparator())
        .limit(10)
        .map(
            entry ->
                new CategoryMetric(
                    entry.getKey(),
                    entry.getValue().revenue(),
                    entry.getValue().orderCount(),
                    entry.getValue().itemCount(),
                    entry.getValue().averageReviewScore()))
        .toList();
  }

  private List<StateMetric> buildRevenueByState(List<OrderItemFact> items) {
    Map<String, SegmentAccumulator> states = new HashMap<>();
    for (OrderItemFact item : items) {
      states.computeIfAbsent(item.customerState(), ignored -> new SegmentAccumulator()).add(item);
    }
    return states.entrySet().stream()
        .sorted(metricComparator())
        .map(
            entry ->
                new StateMetric(
                    entry.getKey(), entry.getValue().revenue(), entry.getValue().orderCount()))
        .toList();
  }

  private List<PaymentMetric> buildPaymentBreakdown(
      List<PaymentFact> payments, Set<String> orderIds, String paymentTypeFilter) {
    Map<String, BigDecimal> totals = new HashMap<>();
    for (PaymentFact payment : payments) {
      if (!orderIds.contains(payment.orderId())) {
        continue;
      }
      if (paymentTypeFilter != null && !same(payment.paymentType(), paymentTypeFilter)) {
        continue;
      }
      totals.merge(payment.paymentType(), payment.paymentValue(), BigDecimal::add);
    }

    BigDecimal total = sum(new ArrayList<>(totals.values()));
    return totals.entrySet().stream()
        .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
        .map(
            entry ->
                new PaymentMetric(
                    entry.getKey(),
                    entry.getValue().setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                    total.signum() == 0
                        ? 0
                        : roundPercent(entry.getValue().doubleValue() / total.doubleValue())))
        .toList();
  }

  private List<ReviewScoreMetric> buildReviewDistribution(Map<String, OrderAccumulator> orders) {
    Map<Integer, Long> counts =
        orders.values().stream()
            .map(OrderAccumulator::reviewScore)
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(score -> score, TreeMap::new, Collectors.counting()));

    List<ReviewScoreMetric> distribution = new ArrayList<>();
    for (int score = 1; score <= 5; score++) {
      distribution.add(new ReviewScoreMetric(score, counts.getOrDefault(score, 0L)));
    }
    return distribution;
  }

  private FilterOptions buildFilterOptions(AnalyticsDataset dataset) {
    TreeSet<String> states = new TreeSet<>();
    TreeSet<String> categories = new TreeSet<>();
    TreeSet<String> paymentTypes = new TreeSet<>();
    LocalDate minDate = null;
    LocalDate maxDate = null;

    for (OrderItemFact item : dataset.items()) {
      states.add(item.customerState());
      categories.add(item.productCategory());
      paymentTypes.addAll(item.paymentTypes());
      if (minDate == null || item.purchaseDate().isBefore(minDate)) {
        minDate = item.purchaseDate();
      }
      if (maxDate == null || item.purchaseDate().isAfter(maxDate)) {
        maxDate = item.purchaseDate();
      }
    }

    return new FilterOptions(
        minDate, maxDate, List.copyOf(states), List.copyOf(categories), List.copyOf(paymentTypes));
  }

  private Comparator<Map.Entry<String, SegmentAccumulator>> metricComparator() {
    return Comparator.<Map.Entry<String, SegmentAccumulator>, BigDecimal>comparing(
            entry -> entry.getValue().revenue())
        .reversed()
        .thenComparing(Map.Entry::getKey);
  }

  private static BigDecimal sum(List<BigDecimal> values) {
    return values.stream()
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
  }

  private static boolean same(String left, String right) {
    return left != null && right != null && left.equalsIgnoreCase(right);
  }

  private static double roundPercent(double value) {
    return roundDouble(value * 100, 1);
  }

  private static double roundDouble(double value, int places) {
    double scale = Math.pow(10, places);
    return Math.round(value * scale) / scale;
  }

  private static final class OrderAccumulator {
    private final String customerUniqueId;
    private Integer reviewScore;
    private boolean deliveryDelayed;

    private OrderAccumulator(OrderItemFact firstItem) {
      this.customerUniqueId = firstItem.customerUniqueId();
    }

    private void add(OrderItemFact item) {
      if (reviewScore == null) {
        reviewScore = item.reviewScore();
      }
      deliveryDelayed = deliveryDelayed || item.deliveryDelayed();
    }

    private String customerUniqueId() {
      return customerUniqueId;
    }

    private Integer reviewScore() {
      return reviewScore;
    }

    private boolean deliveryDelayed() {
      return deliveryDelayed;
    }
  }

  private static final class SegmentAccumulator {
    private BigDecimal revenue = BigDecimal.ZERO;
    private final Set<String> orderIds = new HashSet<>();
    private final Map<String, Integer> reviewScoresByOrder = new HashMap<>();
    private long itemCount;

    private void add(OrderItemFact item) {
      revenue = revenue.add(item.itemRevenue());
      orderIds.add(item.orderId());
      itemCount++;
      if (item.reviewScore() != null) {
        reviewScoresByOrder.putIfAbsent(item.orderId(), item.reviewScore());
      }
    }

    private BigDecimal revenue() {
      return revenue.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private long orderCount() {
      return orderIds.size();
    }

    private long itemCount() {
      return itemCount;
    }

    private Double averageReviewScore() {
      if (reviewScoresByOrder.isEmpty()) {
        return null;
      }
      return roundDouble(
          reviewScoresByOrder.values().stream().mapToInt(Integer::intValue).average().orElse(0), 2);
    }
  }
}
