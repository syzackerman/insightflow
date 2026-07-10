package com.sophia.insightflow.auth;

import java.util.UUID;

public record UserPrincipal(UUID id, String email, String displayName, String role) {}
