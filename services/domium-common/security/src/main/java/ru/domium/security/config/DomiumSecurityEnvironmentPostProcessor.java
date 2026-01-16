package ru.domium.security.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

public class DomiumSecurityEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String issuerUri = environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri");
        if (issuerUri == null || issuerUri.isBlank()) {
            String defaultIssuerUri = environment.getProperty("KEYCLOAK_ISSUER_URI", 
                "http://keycloak.localhost:8080/realms/domium");
            
            Map<String, Object> props = new HashMap<>();
            props.put("spring.security.oauth2.resourceserver.jwt.issuer-uri", defaultIssuerUri);
            
            MapPropertySource propertySource = new MapPropertySource(
                "domium-security-defaults", props);
            environment.getPropertySources().addFirst(propertySource);
        }
    }
}
