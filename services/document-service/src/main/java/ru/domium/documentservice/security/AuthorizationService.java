package ru.domium.documentservice.security;

import ru.domium.documentservice.exception.ApiExceptions;
import ru.domium.documentservice.model.DocumentInstance;
import java.util.UUID;
import org.springframework.stereotype.Component;
import ru.domium.security.util.SecurityUtils;
import org.springframework.security.oauth2.jwt.Jwt;

@Component
public class AuthorizationService {

  public void assertCanReadDocument(Jwt jwt, DocumentInstance doc) {
    if (jwt == null) throw ApiExceptions.forbidden("Unauthenticated");
    if (hasRole(jwt, "builder") || hasRole(jwt, "admin")) return;
    String currentUserId = SecurityUtils.getCurrentUserId(jwt);
    if(currentUserId == null || currentUserId.isBlank())
      throw ApiExceptions.forbidden("Unauthenticated");
    UUID uid = UUID.fromString(currentUserId);
    if (doc.getUserId() == null || !uid.equals(doc.getUserId())) {
      throw ApiExceptions.forbidden("Access denied");
    }
  }

  /*public void assertCanReadProject(Jwt jwt, UUID projectId, UUID userIdInDoc) {
    if (jwt == null) throw ApiExceptions.forbidden("Unauthenticated");
    if (hasRole(jwt, "ROLE_BUILDER") || hasRole(jwt, "ROLE_ADMIN")) return;
    String currentUserId = SecurityUtils.getCurrentUserId(jwt);
    if(currentUserId == null || currentUserId.isBlank())
      throw ApiExceptions.forbidden("Unauthenticated");
    UUID uid = UUID.fromString(currentUserId);
    if (userIdInDoc == null || !uid.equals(userIdInDoc)) {
      throw ApiExceptions.forbidden("Access denied");
    }
  }*/

  public void assertProvider(Jwt jwt) {
    if (jwt == null) throw ApiExceptions.forbidden("Unauthenticated");
    if (hasRole(jwt, "builder") || hasRole(jwt, "admin")) return;
    throw ApiExceptions.forbidden("Builder role required");
  }

  private boolean hasRole(Jwt jwt, String role) {
    return SecurityUtils.hasRole(jwt, role);
  }
}
