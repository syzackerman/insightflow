package com.sophia.insightflow.auth;

import java.time.Instant;
import java.util.UUID;

public record UserAccount(
    UUID id,
    String email,
    String displayName,
    String passwordHash,
    String role,
    Instant createdAt,
    Instant updatedAt) {

  public UserPrincipal toPrincipal() {
    return new UserPrincipal(id, email, displayName, role);
  }
}
