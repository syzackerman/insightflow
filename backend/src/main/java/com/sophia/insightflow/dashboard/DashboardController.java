package com.sophia.insightflow.dashboard;

import com.sophia.insightflow.auth.UserPrincipal;
import com.sophia.insightflow.dashboard.dto.DashboardPreferencesRequest;
import com.sophia.insightflow.dashboard.dto.DashboardPreferencesResponse;
import com.sophia.insightflow.dashboard.dto.SavedDashboardRequest;
import com.sophia.insightflow.dashboard.dto.SavedDashboardResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {

  private final DashboardService dashboardService;

  public DashboardController(DashboardService dashboardService) {
    this.dashboardService = dashboardService;
  }

  @GetMapping("/api/dashboards")
  public List<SavedDashboardResponse> listDashboards(
      @AuthenticationPrincipal UserPrincipal user) {
    return dashboardService.listDashboards(user);
  }

  @PostMapping("/api/dashboards")
  public SavedDashboardResponse createDashboard(
      @AuthenticationPrincipal UserPrincipal user, @RequestBody SavedDashboardRequest request) {
    return dashboardService.createDashboard(user, request);
  }

  @PutMapping("/api/dashboards/{dashboardId}")
  public SavedDashboardResponse updateDashboard(
      @AuthenticationPrincipal UserPrincipal user,
      @PathVariable UUID dashboardId,
      @RequestBody SavedDashboardRequest request) {
    return dashboardService.updateDashboard(user, dashboardId, request);
  }

  @DeleteMapping("/api/dashboards/{dashboardId}")
  public void deleteDashboard(
      @AuthenticationPrincipal UserPrincipal user, @PathVariable UUID dashboardId) {
    dashboardService.deleteDashboard(user, dashboardId);
  }

  @GetMapping("/api/preferences")
  public DashboardPreferencesResponse getPreferences(
      @AuthenticationPrincipal UserPrincipal user) {
    return dashboardService.getPreferences(user);
  }

  @PutMapping("/api/preferences")
  public DashboardPreferencesResponse updatePreferences(
      @AuthenticationPrincipal UserPrincipal user,
      @RequestBody DashboardPreferencesRequest request) {
    return dashboardService.updatePreferences(user, request);
  }
}
