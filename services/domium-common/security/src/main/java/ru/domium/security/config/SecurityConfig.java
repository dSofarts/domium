package ru.domium.security.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

import ru.domium.security.annotation.PublicEndpoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Objects;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityConfig {

    private final DomiumSecurityProperties properties;
    private final ObjectProvider<RequestMappingHandlerMapping> mappingProvider;

    public SecurityConfig(DomiumSecurityProperties properties,
                          @Qualifier("requestMappingHandlerMapping")
                          ObjectProvider<RequestMappingHandlerMapping> mappingProvider) {
        this.properties = properties;
        this.mappingProvider = mappingProvider;
    }

    @Bean
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        List<String> permitAllPatterns = new ArrayList<>(properties.getPermitAll());
        permitAllPatterns.addAll(resolvePublicEndpoints());
        List<String> normalizedPermitAll = permitAllPatterns.stream()
                .map(SecurityConfig::normalizePattern)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(normalizedPermitAll.toArray(String[]::new)).permitAll()
                .anyRequest().authenticated()
            )
            .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()))
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new JwtRoleConverter());
        return converter;
    }

    /**
     * Сканирует контроллеры и собирает пути, помеченные @PublicEndpoint.
     */
    private List<String> resolvePublicEndpoints() {
        RequestMappingHandlerMapping mapping = mappingProvider.getIfAvailable();
        if (mapping == null) {
            return List.of();
        }

        return mapping.getHandlerMethods().entrySet().stream()
            .filter(entry -> isPublic(entry.getValue().getMethod().getDeclaringClass(), entry.getValue().getMethod()))
            .flatMap(entry -> extractPaths(entry.getKey(), entry.getValue().getMethod()).stream())
            .distinct()
            .toList();
    }

    private boolean isPublic(Class<?> controllerClass, java.lang.reflect.Method method) {
        return method.isAnnotationPresent(PublicEndpoint.class)
                || controllerClass.isAnnotationPresent(PublicEndpoint.class);
    }

    private List<String> extractPaths(RequestMappingInfo info, java.lang.reflect.Method method) {
        PublicEndpoint annotation = method.getAnnotation(PublicEndpoint.class);
        if (annotation == null) {
            annotation = method.getDeclaringClass().getAnnotation(PublicEndpoint.class);
        }
        if (annotation != null && annotation.paths().length > 0) {
            return java.util.Arrays.stream(annotation.paths())
                    .map(SecurityConfig::normalizePattern)
                    .toList();
        }

        if (info.getPathPatternsCondition() != null) {
            Set<PathPattern> patterns = info.getPathPatternsCondition().getPatterns();
            return patterns.stream()
                    .map(PathPattern::getPatternString)
                    .map(SecurityConfig::normalizePattern)
                    .collect(Collectors.toList());
        }
        if (info.getPatternsCondition() != null) {
            return info.getPatternsCondition().getPatterns().stream()
                    .map(SecurityConfig::normalizePattern)
                    .toList();
        }

        return List.of();
    }

    private static String normalizePattern(String pattern) {
        if (pattern == null) {
            return null;
        }
        String trimmed = pattern.trim();
        if (trimmed.isEmpty()) {
            return "/";
        }
        if (!trimmed.startsWith("/")) {
            return "/" + trimmed;
        }
        return trimmed;
    }
}

