package com.sophia.insightflow.ai.dto;

import com.sophia.insightflow.analytics.dto.AnalyticsFilterRequest;

public record NaturalLanguageQueryRequest(String question, AnalyticsFilterRequest filters) {}
