package com.sophia.insightflow.analytics.dto;

import java.time.Instant;

public record AnalyticsErrorResponse(Instant timestamp, int status, String error, String message) {}
