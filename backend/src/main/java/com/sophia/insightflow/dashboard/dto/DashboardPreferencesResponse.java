package com.sophia.insightflow.dashboard.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DashboardPreferencesResponse(
    String theme,
    boolean compactView,
    UUID defaultDashboardId,
    List<String> visibleSections,
    Instant updatedAt) {}
