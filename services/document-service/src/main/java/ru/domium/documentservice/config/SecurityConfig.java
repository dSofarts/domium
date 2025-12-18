package ru.domium.documentservice.config;

import java.util.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health", "/actuator/info").permitAll()
            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**",  "/swagger-ui.html").permitAll()
            .requestMatchers("/actuator/prometheus").permitAll()
            .anyRequest().authenticated()
        )
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())));
    return http.build();
  }

  @Bean
  JwtAuthenticationConverter jwtAuthConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
    return converter;
  }

  static class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    @Override
    @SuppressWarnings("unchecked")
    public Collection<GrantedAuthority> convert(Jwt jwt) {
      Set<GrantedAuthority> authorities = new HashSet<>();

      // Keycloak: realm_access.roles
      Map<String, Object> realmAccess = jwt.getClaim("realm_access");
      if (realmAccess != null) {
        Object roles = realmAccess.get("roles");
        if (roles instanceof Collection<?> roleList) {
          for (Object roleObj : roleList) {
            String role = String.valueOf(roleObj).toUpperCase(Locale.ROOT);
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
          }
        }
      }

      // Optional: support "roles" claim
      Object roles = jwt.getClaim("roles");
      if (roles instanceof Collection<?> roleList) {
        for (Object roleObj : roleList) {
          String role = String.valueOf(roleObj).toUpperCase(Locale.ROOT);
          authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
      }

      return authorities;
    }
  }
}
