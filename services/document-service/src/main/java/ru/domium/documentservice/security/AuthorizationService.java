package ru.domium.documentservice.security;

import ru.domium.documentservice.exception.ApiExceptions;
import ru.domium.documentservice.model.DocumentInstance;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class AuthorizationService {

  public void assertCanReadDocument(Authentication auth, DocumentInstance doc) {
    if (auth == null) throw ApiExceptions.forbidden("Unauthenticated");
    if (hasRole(auth, "ROLE_BUILDER") || hasRole(auth, "ROLE_ADMIN")) return;
    UUID uid = UserContext.userId(auth);
    if (uid == null || doc.getUserId() == null || !uid.equals(doc.getUserId())) {
      throw ApiExceptions.forbidden("Access denied");
    }
  }

  public void assertCanReadProject(Authentication auth, UUID projectId, UUID userIdInDoc) {
    if (auth == null) throw ApiExceptions.forbidden("Unauthenticated");
    if (hasRole(auth, "ROLE_BUILDER") || hasRole(auth, "ROLE_ADMIN")) return;
    UUID uid = UserContext.userId(auth);
    if (uid == null || userIdInDoc == null || !uid.equals(userIdInDoc)) {
      throw ApiExceptions.forbidden("Access denied");
    }
  }

  public void assertProvider(Authentication auth) {
    if (auth == null) throw ApiExceptions.forbidden("Unauthenticated");
    if (hasRole(auth, "ROLE_BUILDER") || hasRole(auth, "ROLE_ADMIN")) return;
    throw ApiExceptions.forbidden("Builder role required");
  }

  private boolean hasRole(Authentication auth, String role) {
    return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
  }
}
