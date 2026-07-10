package com.sophia.insightflow.ai.dto;

import java.time.Instant;
import java.util.List;

public record ExecutiveReportResponse(
    Instant generatedAt,
    String mode,
    String analyticsSource,
    String title,
    String executiveSummary,
    List<String> keyFindings,
    List<String> risks,
    List<String> recommendations) {}
