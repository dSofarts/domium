package ru.domium.security.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Конвертер для преобразования ролей из Keycloak JWT токена в Spring Security authorities.
 * Читает роли из realm_access.roles и преобразует их в формат ROLE_*.
 */
public class JwtRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = defaultConverter.convert(jwt);

        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        Collection<GrantedAuthority> realmRoles = List.of();
        if (realmAccess != null) {
            List<String> roles = (List<String>) realmAccess.get("roles");
            if (roles != null) {
                realmRoles = roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                        .collect(Collectors.toList());
            }
        }

        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        Collection<GrantedAuthority> resourceRoles = List.of();
        if (resourceAccess != null) {
            resourceRoles = resourceAccess.values().stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .flatMap(entry -> {
                        Object roles = entry.get("roles");
                        if (roles instanceof List) {
                            return ((List<?>) roles).stream()
                                    .filter(String.class::isInstance)
                                    .map(String.class::cast)
                                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                        }
                        return Stream.empty();
                    })
                    .collect(Collectors.toList());
        }

        return Stream.of(authorities, realmRoles, resourceRoles)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }
}

