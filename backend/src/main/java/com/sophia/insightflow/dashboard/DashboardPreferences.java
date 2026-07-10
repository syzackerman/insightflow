package com.sophia.insightflow.dashboard;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DashboardPreferences(
    UUID userId,
    String theme,
    boolean compactView,
    UUID defaultDashboardId,
    List<String> visibleSections,
    Instant updatedAt) {

  public static DashboardPreferences defaults(UUID userId) {
    return new DashboardPreferences(
        userId,
        "light",
        false,
        null,
        List.of("kpis", "ai", "trend", "categories", "states", "reviews", "payments"),
        Instant.now());
  }
}
