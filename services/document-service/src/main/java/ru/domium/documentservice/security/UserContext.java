package ru.domium.documentservice.security;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

public final class UserContext {
  private UserContext() {}

  public static UUID userId(Authentication auth) {
    if (auth == null) return null;
    Object principal = auth.getPrincipal();
    if (principal instanceof Jwt jwt) {
      // common: sub is UUID string
      String sub = jwt.getSubject();
      try {
        return UUID.fromString(sub);
      } catch (Exception ignored) {
        // fallback: custom claim
        String userId = jwt.getClaimAsString("user_id");
        if (userId == null) return null;
        try { return UUID.fromString(userId); } catch (Exception e) { return null; }
      }
    }
    return null;
  }
}
