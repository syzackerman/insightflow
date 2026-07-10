package com.sophia.insightflow.analytics;

import com.sophia.insightflow.analytics.dto.AnalyticsFilterRequest;
import com.sophia.insightflow.analytics.dto.AnalyticsSummaryResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@Validated
public class AnalyticsController {

  private final AnalyticsSummaryService analyticsSummaryService;

  public AnalyticsController(AnalyticsSummaryService analyticsSummaryService) {
    this.analyticsSummaryService = analyticsSummaryService;
  }

  @GetMapping("/summary")
  public AnalyticsSummaryResponse summary(@ModelAttribute AnalyticsFilterRequest request) {
    return analyticsSummaryService.summarize(request.toFilter());
  }
}
