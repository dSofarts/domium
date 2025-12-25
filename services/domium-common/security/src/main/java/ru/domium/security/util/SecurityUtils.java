package ru.domium.security.util;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

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
}

