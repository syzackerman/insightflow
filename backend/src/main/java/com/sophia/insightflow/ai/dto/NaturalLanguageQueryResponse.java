package com.sophia.insightflow.ai.dto;

import java.time.Instant;
import java.util.List;

public record NaturalLanguageQueryResponse(
    Instant generatedAt,
    String mode,
    String analyticsSource,
    String question,
    String answer,
    List<String> supportingMetrics,
    List<String> followUpQuestions) {}
