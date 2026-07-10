package com.sophia.insightflow.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sophia.insightflow.ai.dto.ExecutiveReportResponse;
import com.sophia.insightflow.ai.dto.NaturalLanguageQueryRequest;
import com.sophia.insightflow.ai.dto.NaturalLanguageQueryResponse;
import com.sophia.insightflow.analytics.AnalyticsSummaryService;
import com.sophia.insightflow.analytics.dto.AnalyticsFilter;
import com.sophia.insightflow.analytics.dto.AnalyticsFilterRequest;
import com.sophia.insightflow.analytics.dto.AnalyticsSummaryResponse;
import com.sophia.insightflow.analytics.dto.CategoryMetric;
import com.sophia.insightflow.analytics.dto.MonthlyRevenueMetric;
import com.sophia.insightflow.analytics.dto.PaymentMetric;
import com.sophia.insightflow.analytics.dto.StateMetric;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AiBusinessAnalystService {

  private static final AnalyticsFilter EMPTY_FILTER =
      new AnalyticsFilter(null, null, null, null, null);

  private final AnalyticsSummaryService analyticsSummaryService;
  private final OpenAiAnalystClient openAiAnalystClient;
  private final ObjectMapper objectMapper;

  public AiBusinessAnalystService(
      AnalyticsSummaryService analyticsSummaryService,
      OpenAiAnalystClient openAiAnalystClient,
      ObjectMapper objectMapper) {
    this.analyticsSummaryService = analyticsSummaryService;
    this.openAiAnalystClient = openAiAnalystClient;
    this.objectMapper = objectMapper;
  }

  public ExecutiveReportResponse generateExecutiveReport(AnalyticsFilterRequest request) {
    AnalyticsFilter filter = toFilter(request);
    AnalyticsSummaryResponse summary = analyticsSummaryService.summarize(filter);
    LocalReport localReport = buildLocalReport(summary);
    OpenAiAnalystClient.LlmCompletion completion =
        openAiAnalystClient.complete(reportInstructions(), reportPrompt(summary, localReport));

    return new ExecutiveReportResponse(
        Instant.now(),
        completion.usedLlm() ? "openai" : "local",
        summary.source(),
        "InsightFlow Executive Brief",
        completion.usedLlm() ? completion.text() : localReport.executiveSummary(),
        localReport.keyFindings(),
        localReport.risks(),
        localReport.recommendations());
  }

  public NaturalLanguageQueryResponse answerQuestion(NaturalLanguageQueryRequest request) {
    if (request == null || request.question() == null || request.question().isBlank()) {
      throw new IllegalArgumentException("question is required");
    }

    AnalyticsFilter filter = toFilter(request.filters());
    AnalyticsSummaryResponse summary = analyticsSummaryService.summarize(filter);
    String localAnswer = localAnswer(request.question(), summary);
    List<String> supportingMetrics = supportingMetrics(summary);
    OpenAiAnalystClient.LlmCompletion completion =
        openAiAnalystClient.complete(queryInstructions(), queryPrompt(request.question(), summary));

    return new NaturalLanguageQueryResponse(
        Instant.now(),
        completion.usedLlm() ? "openai" : "local",
        summary.source(),
        request.question().trim(),
        completion.usedLlm() ? completion.text() : localAnswer,
        supportingMetrics,
        List.of(
            "Which category should receive more investment?",
            "Where are delivery delays highest?",
            "How is revenue trending over time?"));
  }

  private AnalyticsFilter toFilter(AnalyticsFilterRequest request) {
    if (request == null) {
      return EMPTY_FILTER;
    }
    return request.toFilter();
  }

  private LocalReport buildLocalReport(AnalyticsSummaryResponse summary) {
    List<String> keyFindings = new ArrayList<>();
    List<String> risks = new ArrayList<>();
    List<String> recommendations = new ArrayList<>();

    keyFindings.add(
        "Delivered orders generated "
            + money(summary.kpis().totalRevenue())
            + " across "
            + number(summary.kpis().totalOrders())
            + " orders.");

    topCategory(summary)
        .ifPresent(
            category ->
                keyFindings.add(
                    category.category()
                        + " is the top category with "
                        + money(category.revenue())
                        + " in revenue."));

    topState(summary)
        .ifPresent(
            state ->
                keyFindings.add(
                    state.state()
                        + " is the leading customer state with "
                        + money(state.revenue())
                        + " in revenue."));

    monthlyTrend(summary)
        .ifPresent(
            trend ->
                keyFindings.add(
                    "Monthly revenue finished "
                        + trend.direction()
                        + " by "
                        + money(trend.absoluteChange())
                        + " versus the first visible month."));

    if (summary.kpis().deliveryDelayRate() >= 10) {
      risks.add(
          "Delivery delay rate is "
              + percent(summary.kpis().deliveryDelayRate())
              + ", which may affect repeat purchase behavior.");
    } else {
      risks.add(
          "Delivery delay rate is controlled at "
              + percent(summary.kpis().deliveryDelayRate())
              + ".");
    }

    if (summary.kpis().averageReviewScore() != null && summary.kpis().averageReviewScore() < 4.0) {
      risks.add("Average review score is below 4.0, signaling customer experience pressure.");
    } else {
      risks.add("Average review score is healthy relative to a 5-star scale.");
    }

    recommendations.add("Prioritize merchandising and inventory for the highest-revenue categories.");
    recommendations.add("Review delivery operations in high-volume states before scaling campaigns.");
    recommendations.add("Use payment mix and AOV to tune promotions without eroding margin.");

    String executiveSummary =
        "Revenue is concentrated in the strongest categories and regions, with delivery reliability "
            + "and review quality acting as the main operating checks. The next move is to protect "
            + "customer experience while investing in the segments already proving demand.";

    return new LocalReport(executiveSummary, keyFindings, risks, recommendations);
  }

  private String localAnswer(String question, AnalyticsSummaryResponse summary) {
    String normalized = question.toLowerCase(Locale.ROOT);
    if (summary.empty()) {
      return "No orders match the current filters, so there is not enough data to answer that question.";
    }
    if (normalized.contains("category") || normalized.contains("product")) {
      return topCategory(summary)
          .map(
              category ->
                  category.category()
                      + " leads product performance with "
                      + money(category.revenue())
                      + " in revenue and "
                      + number(category.orders())
                      + " orders.")
          .orElse("No category revenue is available for the selected filters.");
    }
    if (normalized.contains("state") || normalized.contains("region") || normalized.contains("where")) {
      return topState(summary)
          .map(
              state ->
                  state.state()
                      + " is the strongest region with "
                      + money(state.revenue())
                      + " in revenue across "
                      + number(state.orders())
                      + " orders.")
          .orElse("No regional revenue is available for the selected filters.");
    }
    if (normalized.contains("payment")) {
      return topPayment(summary)
          .map(
              payment ->
                  payment.paymentType()
                      + " is the largest payment method at "
                      + percent(payment.share())
                      + " of payment value.")
          .orElse("No payment breakdown is available for the selected filters.");
    }
    if (normalized.contains("review") || normalized.contains("rating")) {
      return "Average review score is "
          + (summary.kpis().averageReviewScore() == null
              ? "not available"
              : String.format(Locale.US, "%.2f out of 5", summary.kpis().averageReviewScore()))
          + ".";
    }
    if (normalized.contains("delay") || normalized.contains("delivery")) {
      return "Delivery delay rate is "
          + percent(summary.kpis().deliveryDelayRate())
          + ", so delivery reliability should remain part of the operating review.";
    }
    if (normalized.contains("trend") || normalized.contains("month")) {
      return monthlyTrend(summary)
          .map(
              trend ->
                  "Revenue is "
                      + trend.direction()
                      + " by "
                      + money(trend.absoluteChange())
                      + " from the first visible month to the latest visible month.")
          .orElse("Monthly revenue trend data is not available for the selected filters.");
    }
    return "The filtered view shows "
        + money(summary.kpis().totalRevenue())
        + " in revenue, "
        + number(summary.kpis().totalOrders())
        + " delivered orders, and an average order value of "
        + money(summary.kpis().averageOrderValue())
        + ".";
  }

  private String reportInstructions() {
    return """
        You are InsightFlow's AI Business Analyst. Write a concise executive report for an ecommerce dashboard.
        Use only the supplied analytics. Do not invent causes, customer names, or external market facts.
        Keep the tone board-ready, specific, and action-oriented.
        """;
  }

  private String queryInstructions() {
    return """
        You are InsightFlow's AI Business Analyst. Answer the user's analytics question using only the supplied metrics.
        If the supplied metrics do not answer the question, say what is missing and suggest the nearest available metric.
        Keep the answer concise.
        """;
  }

  private String reportPrompt(AnalyticsSummaryResponse summary, LocalReport localReport) {
    return "Analytics summary:\n"
        + compactSummaryJson(summary)
        + "\n\nComputed findings:\n"
        + String.join("\n", localReport.keyFindings())
        + "\n\nWrite a 2-3 paragraph executive report.";
  }

  private String queryPrompt(String question, AnalyticsSummaryResponse summary) {
    return "Question: "
        + question.trim()
        + "\n\nAnalytics summary:\n"
        + compactSummaryJson(summary)
        + "\n\nAnswer the question directly.";
  }

  private String compactSummaryJson(AnalyticsSummaryResponse summary) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("source", summary.source());
    payload.put("filters", summary.filters());
    payload.put("kpis", summary.kpis());
    payload.put("revenueByMonth", summary.revenueByMonth());
    payload.put("topCategories", summary.topCategories());
    payload.put("revenueByState", summary.revenueByState());
    payload.put("paymentMethodBreakdown", summary.paymentMethodBreakdown());
    payload.put("reviewDistribution", summary.reviewDistribution());
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException error) {
      return payload.toString();
    }
  }

  private List<String> supportingMetrics(AnalyticsSummaryResponse summary) {
    List<String> metrics = new ArrayList<>();
    metrics.add("Revenue: " + money(summary.kpis().totalRevenue()));
    metrics.add("Orders: " + number(summary.kpis().totalOrders()));
    metrics.add("Average order value: " + money(summary.kpis().averageOrderValue()));
    metrics.add("Repeat customer rate: " + percent(summary.kpis().repeatCustomerRate()));
    metrics.add("Delivery delay rate: " + percent(summary.kpis().deliveryDelayRate()));
    if (summary.kpis().averageReviewScore() != null) {
      metrics.add(
          "Average review score: "
              + String.format(Locale.US, "%.2f", summary.kpis().averageReviewScore()));
    }
    return metrics;
  }

  private java.util.Optional<CategoryMetric> topCategory(AnalyticsSummaryResponse summary) {
    return summary.topCategories().stream().findFirst();
  }

  private java.util.Optional<StateMetric> topState(AnalyticsSummaryResponse summary) {
    return summary.revenueByState().stream().findFirst();
  }

  private java.util.Optional<PaymentMetric> topPayment(AnalyticsSummaryResponse summary) {
    return summary.paymentMethodBreakdown().stream().findFirst();
  }

  private java.util.Optional<RevenueTrend> monthlyTrend(AnalyticsSummaryResponse summary) {
    List<MonthlyRevenueMetric> months = summary.revenueByMonth();
    if (months.size() < 2) {
      return java.util.Optional.empty();
    }
    BigDecimal first = months.get(0).revenue();
    BigDecimal last = months.get(months.size() - 1).revenue();
    BigDecimal change = last.subtract(first);
    return java.util.Optional.of(
        new RevenueTrend(change.signum() >= 0 ? "up" : "down", change.abs()));
  }

  private String money(BigDecimal value) {
    return "$" + number(value.setScale(0, RoundingMode.HALF_UP).longValue());
  }

  private String number(long value) {
    return String.format(Locale.US, "%,d", value);
  }

  private String percent(double value) {
    return String.format(Locale.US, "%.1f%%", value);
  }

  private record LocalReport(
      String executiveSummary,
      List<String> keyFindings,
      List<String> risks,
      List<String> recommendations) {}

  private record RevenueTrend(String direction, BigDecimal absoluteChange) {}
}
