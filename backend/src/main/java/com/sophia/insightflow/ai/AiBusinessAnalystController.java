package com.sophia.insightflow.ai;

import com.sophia.insightflow.ai.dto.ExecutiveReportRequest;
import com.sophia.insightflow.ai.dto.ExecutiveReportResponse;
import com.sophia.insightflow.ai.dto.NaturalLanguageQueryRequest;
import com.sophia.insightflow.ai.dto.NaturalLanguageQueryResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@Validated
public class AiBusinessAnalystController {

  private final AiBusinessAnalystService aiBusinessAnalystService;

  public AiBusinessAnalystController(AiBusinessAnalystService aiBusinessAnalystService) {
    this.aiBusinessAnalystService = aiBusinessAnalystService;
  }

  @PostMapping("/report")
  public ExecutiveReportResponse report(@RequestBody(required = false) ExecutiveReportRequest request) {
    return aiBusinessAnalystService.generateExecutiveReport(
        request == null ? null : request.filters());
  }

  @PostMapping("/query")
  public NaturalLanguageQueryResponse query(
      @RequestBody NaturalLanguageQueryRequest request) {
    return aiBusinessAnalystService.answerQuestion(request);
  }
}
