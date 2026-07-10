package com.sophia.insightflow.dashboard;

import com.sophia.insightflow.analytics.dto.AnalyticsFilter;
import com.sophia.insightflow.auth.UserPrincipal;
import com.sophia.insightflow.dashboard.dto.DashboardPreferencesRequest;
import com.sophia.insightflow.dashboard.dto.DashboardPreferencesResponse;
import com.sophia.insightflow.dashboard.dto.SavedDashboardRequest;
import com.sophia.insightflow.dashboard.dto.SavedDashboardResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

  private final SavedDashboardRepository savedDashboardRepository;
  private final DashboardPreferencesRepository dashboardPreferencesRepository;

  public DashboardService(
      SavedDashboardRepository savedDashboardRepository,
      DashboardPreferencesRepository dashboardPreferencesRepository) {
    this.savedDashboardRepository = savedDashboardRepository;
    this.dashboardPreferencesRepository = dashboardPreferencesRepository;
  }

  public List<SavedDashboardResponse> listDashboards(UserPrincipal user) {
    return savedDashboardRepository.findAllByUserId(user.id()).stream().map(this::toResponse).toList();
  }

  public SavedDashboardResponse createDashboard(UserPrincipal user, SavedDashboardRequest request) {
    Instant now = Instant.now();
    SavedDashboard dashboard =
        new SavedDashboard(
            UUID.randomUUID(),
            user.id(),
            cleanName(request.name()),
            cleanDescription(request.description()),
            filters(request),
            now,
            now);
    return toResponse(savedDashboardRepository.save(dashboard));
  }

  public SavedDashboardResponse updateDashboard(
      UserPrincipal user, UUID dashboardId, SavedDashboardRequest request) {
    SavedDashboard existing =
        savedDashboardRepository
            .findByIdAndUserId(dashboardId, user.id())
            .orElseThrow(() -> new IllegalArgumentException("Saved dashboard not found"));
    SavedDashboard dashboard =
        new SavedDashboard(
            existing.id(),
            existing.userId(),
            cleanName(request.name()),
            cleanDescription(request.description()),
            filters(request),
            existing.createdAt(),
            Instant.now());
    return toResponse(savedDashboardRepository.save(dashboard));
  }

  public void deleteDashboard(UserPrincipal user, UUID dashboardId) {
    savedDashboardRepository.delete(dashboardId, user.id());
  }

  public DashboardPreferencesResponse getPreferences(UserPrincipal user) {
    return toResponse(
        dashboardPreferencesRepository
            .findByUserId(user.id())
            .orElseGet(() -> dashboardPreferencesRepository.save(DashboardPreferences.defaults(user.id()))));
  }

  public DashboardPreferencesResponse updatePreferences(
      UserPrincipal user, DashboardPreferencesRequest request) {
    DashboardPreferences current =
        dashboardPreferencesRepository
            .findByUserId(user.id())
            .orElseGet(() -> DashboardPreferences.defaults(user.id()));
    UUID defaultDashboardId = request.defaultDashboardId();
    if (defaultDashboardId != null
        && savedDashboardRepository.findByIdAndUserId(defaultDashboardId, user.id()).isEmpty()) {
      throw new IllegalArgumentException("defaultDashboardId must reference one of your saved dashboards");
    }
    DashboardPreferences updated =
        new DashboardPreferences(
            user.id(),
            cleanTheme(request.theme(), current.theme()),
            request.compactView() == null ? current.compactView() : request.compactView(),
            defaultDashboardId,
            request.visibleSections() == null ? current.visibleSections() : request.visibleSections(),
            Instant.now());
    return toResponse(dashboardPreferencesRepository.save(updated));
  }

  private AnalyticsFilter filters(SavedDashboardRequest request) {
    if (request.filters() == null) {
      return new AnalyticsFilter(null, null, null, null, null);
    }
    return request.filters().toFilter();
  }

  private String cleanName(String name) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Dashboard name is required");
    }
    return name.trim();
  }

  private String cleanDescription(String description) {
    return description == null || description.isBlank() ? null : description.trim();
  }

  private String cleanTheme(String requestedTheme, String currentTheme) {
    if (requestedTheme == null || requestedTheme.isBlank()) {
      return currentTheme == null ? "light" : currentTheme;
    }
    String theme = requestedTheme.trim().toLowerCase();
    if (!theme.equals("light") && !theme.equals("focus")) {
      throw new IllegalArgumentException("theme must be light or focus");
    }
    return theme;
  }

  private SavedDashboardResponse toResponse(SavedDashboard dashboard) {
    return new SavedDashboardResponse(
        dashboard.id(),
        dashboard.name(),
        dashboard.description(),
        dashboard.filters(),
        dashboard.createdAt(),
        dashboard.updatedAt());
  }

  private DashboardPreferencesResponse toResponse(DashboardPreferences preferences) {
    return new DashboardPreferencesResponse(
        preferences.theme(),
        preferences.compactView(),
        preferences.defaultDashboardId(),
        preferences.visibleSections(),
        preferences.updatedAt());
  }
}
