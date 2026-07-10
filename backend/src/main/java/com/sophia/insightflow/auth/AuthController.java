package com.sophia.insightflow.auth;

import com.sophia.insightflow.auth.dto.AuthResponse;
import com.sophia.insightflow.auth.dto.LoginRequest;
import com.sophia.insightflow.auth.dto.RegisterRequest;
import com.sophia.insightflow.auth.dto.UserProfileResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/register")
  public AuthResponse register(@RequestBody RegisterRequest request) {
    return authService.register(request);
  }

  @PostMapping("/login")
  public AuthResponse login(@RequestBody LoginRequest request) {
    return authService.login(request);
  }

  @GetMapping("/me")
  public UserProfileResponse me(@AuthenticationPrincipal UserPrincipal principal) {
    return authService.profile(principal);
  }
}
