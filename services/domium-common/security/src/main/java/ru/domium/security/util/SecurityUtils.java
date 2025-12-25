package ru.domium.security.util;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Предоставляет методы для получения информации о пользователе из JWT.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * Получает ID пользователя из JWT токена
     * @param jwt JWT токен
     * @return subject (sub) из JWT токена
     */
    public static String getCurrentUserId(Jwt jwt) {
        return jwt.getSubject();
    }

    /**
     * Возвращает subject (sub) как UUID.
     * @throws IllegalArgumentException если subject отсутствует или не UUID
     */
    public static UUID requireSubjectUuid(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new IllegalArgumentException("JWT subject is missing");
        }
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("JWT subject is not a UUID: " + jwt.getSubject(), e);
        }
    }

    /**
     * Имя пользователя из JWT.
     * Порядок: name -> preferred_username -> email -> sub.
     */
    public static String resolveDisplayName(Jwt jwt) {
        if (jwt == null) return null;
        String name = jwt.getClaimAsString("name");
        if (name != null && !name.isBlank()) return name;
        String preferred = jwt.getClaimAsString("preferred_username");
        if (preferred != null && !preferred.isBlank()) return preferred;
        String email = jwt.getClaimAsString("email");
        if (email != null && !email.isBlank()) return email;
        return jwt.getSubject();
    }

    /**
     * Проверяет, имеет ли пользователь указанную роль
     * @param jwt JWT токен
     * @param role роль для проверки (без префикса ROLE_)
     * @return true, если пользователь имеет указанную роль
     */
    @SuppressWarnings("unchecked")
    public static boolean hasRole(Jwt jwt, String role) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null) {
            Object rolesObj = realmAccess.get("roles");
            if (rolesObj instanceof List) {
                List<String> roles = (List<String>) rolesObj;
                return roles.contains(role);
            }
        }
        return false;
    }

    /**
     * Как {@link #hasRole(Jwt, String)}, но сравнение без учета регистра и без требований к типу коллекции.
     */
    public static boolean hasRoleIgnoreCase(Jwt jwt, String role) {
        if (jwt == null || role == null) return false;
        Object realmAccessObj = jwt.getClaim("realm_access");
        if (realmAccessObj instanceof Map<?, ?> realmAccess) {
            Object rolesObj = realmAccess.get("roles");
            if (rolesObj instanceof Collection<?> roles) {
                for (Object r : roles) {
                    if (r != null && role.equalsIgnoreCase(String.valueOf(r))) return true;
                }
            }
        }
        return false;
    }
}
