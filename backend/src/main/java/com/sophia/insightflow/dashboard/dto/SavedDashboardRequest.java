package com.sophia.insightflow.dashboard.dto;

import com.sophia.insightflow.analytics.dto.AnalyticsFilterRequest;

public record SavedDashboardRequest(
    String name, String description, AnalyticsFilterRequest filters) {}
