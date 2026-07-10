package com.sophia.insightflow.dashboard.dto;

import com.sophia.insightflow.analytics.dto.AnalyticsFilter;
import java.time.Instant;
import java.util.UUID;

public record SavedDashboardResponse(
    UUID id,
    String name,
    String description,
    AnalyticsFilter filters,
    Instant createdAt,
    Instant updatedAt) {}
