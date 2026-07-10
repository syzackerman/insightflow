package com.sophia.insightflow.auth.dto;

public record AuthResponse(String token, UserProfileResponse user) {}
