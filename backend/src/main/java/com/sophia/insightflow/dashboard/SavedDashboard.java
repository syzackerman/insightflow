package com.sophia.insightflow.dashboard;

import com.sophia.insightflow.analytics.dto.AnalyticsFilter;
import java.time.Instant;
import java.util.UUID;

public record SavedDashboard(
    UUID id,
    UUID userId,
    String name,
    String description,
    AnalyticsFilter filters,
    Instant createdAt,
    Instant updatedAt) {}
