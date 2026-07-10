package com.sophia.insightflow.dashboard.dto;

import java.util.List;
import java.util.UUID;

public record DashboardPreferencesRequest(
    String theme, Boolean compactView, UUID defaultDashboardId, List<String> visibleSections) {}
