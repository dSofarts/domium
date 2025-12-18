package ru.domium.gateway.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/v3/api-docs")
@RequiredArgsConstructor
public class OpenApiAggregationController {

    private final WebClient webClient;
    private final DiscoveryClient discoveryClient;
    private final RouteDefinitionLocator routeDefinitionLocator;
    
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${gateway.server.url:http://localhost:8080}")
    private String gatewayServerUrl;

    @GetMapping("/aggregated")
    public Mono<ResponseEntity<Map<String, Object>>> getAggregatedApiDocs() {
        return collectAllApiDocs()
                .collectList()
                .map(docs -> {
                    Map<String, Object> aggregated = new HashMap<>();
                    aggregated.put("openapi", "3.0.1");
                    aggregated.put("info", Map.of(
                            "title", "Domium API Gateway - Unified API Documentation",
                            "version", "1.0.0",
                            "description", "Объединенная документация API всех микросервисов"
                    ));
                    
                    Map<String, Object> paths = new HashMap<>();
                    Map<String, Object> components = new HashMap<>();
                    Map<String, Object> schemas = new HashMap<>();
                    Map<String, Object> securitySchemes = new HashMap<>();
                    List<Map<String, Object>> tags = new ArrayList<>();
                    Set<String> tagNames = new HashSet<>();

                    for (JsonNode doc : docs) {
                        if (doc.has("paths")) {
                            doc.get("paths").fields().forEachRemaining(entry -> {
                                String originalPath = entry.getKey();
                                paths.put(originalPath, entry.getValue());
                            });
                        }
                        if (doc.has("components")) {
                            JsonNode componentsNode = doc.get("components");
                            if (componentsNode.has("schemas")) {
                                componentsNode.get("schemas").fields().forEachRemaining(entry -> {
                                    String schemaName = entry.getKey();
                                    if (schemas.containsKey(schemaName)) {
                                        schemaName = schemaName + "_" + System.currentTimeMillis();
                                    }
                                    schemas.put(schemaName, entry.getValue());
                                });
                            }
                            if (componentsNode.has("securitySchemes")) {
                                componentsNode.get("securitySchemes").fields().forEachRemaining(entry -> {
                                    String schemeName = entry.getKey();
                                    if (!securitySchemes.containsKey(schemeName)) {
                                        securitySchemes.put(schemeName, objectMapper.convertValue(entry.getValue(), Map.class));
                                    }
                                });
                            }
                        }
                        if (doc.has("tags")) {
                            doc.get("tags").forEach(tag -> {
                                String tagName = tag.get("name").asText();
                                if (!tagNames.contains(tagName)) {
                                    tagNames.add(tagName);
                                    tags.add(objectMapper.convertValue(tag, Map.class));
                                }
                            });
                        }
                    }

                    if (!securitySchemes.containsKey("bearer-jwt")) {
                        Map<String, Object> bearerJwtScheme = new HashMap<>();
                        bearerJwtScheme.put("type", "http");
                        bearerJwtScheme.put("scheme", "bearer");
                        bearerJwtScheme.put("bearerFormat", "JWT");
                        bearerJwtScheme.put("description", "JWT токен из Keycloak. Получите токен через Keycloak и вставьте его в формате: Bearer {token}");
                        securitySchemes.put("bearer-jwt", bearerJwtScheme);
                    }

                    components.put("schemas", schemas);
                    if (!securitySchemes.isEmpty()) {
                        components.put("securitySchemes", securitySchemes);
                    }
                    aggregated.put("paths", paths);
                    aggregated.put("components", components);
                    aggregated.put("tags", tags);

                    List<Map<String, List<String>>> security = List.of(
                            Map.of("bearer-jwt", List.of())
                    );
                    aggregated.put("security", security);

                    return ResponseEntity.ok(aggregated);
                })
                .onErrorResume(error -> {
                    log.error("Error aggregating API docs", error);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    private Flux<JsonNode> collectAllApiDocs() {
        List<String> services = discoveryClient.getServices().stream()
                .filter(service -> !service.equals("api-gateway"))
                .collect(Collectors.toList());

        log.info("Found services for API docs aggregation: {}", services);

        return Flux.fromIterable(services)
                .flatMap(service -> fetchApiDocs(service)
                        .doOnError(error -> log.warn("Failed to fetch API docs from service: {}", service, error))
                        .onErrorResume(error -> Mono.empty())
                );
    }

    private Mono<JsonNode> fetchApiDocs(String serviceName) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
        
        if (instances.isEmpty()) {
            log.debug("No instances found for service: {}", serviceName);
            return Mono.empty();
        }

        ServiceInstance instance = instances.get(0);
        String baseUrl = String.format("http://%s:%d", instance.getHost(), instance.getPort());
        String url = baseUrl + "/v3/api-docs";
        
        log.debug("Fetching API docs from service: {} at {}", serviceName, url);
        
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(json -> {
                    try {
                        JsonNode doc = objectMapper.readTree(json);
                        return Mono.just(updateServerUrls(doc, serviceName));
                    } catch (Exception e) {
                        log.error("Error parsing JSON from service: {}", serviceName, e);
                        return Mono.just(objectMapper.createObjectNode());
                    }
                })
                .timeout(java.time.Duration.ofSeconds(5))
                .onErrorResume(error -> {
                    log.debug("Service {} does not provide API docs or is unavailable: {}", serviceName, error.getMessage());
                    return Mono.empty();
                });
    }
    
    private JsonNode updateServerUrls(JsonNode doc, String serviceName) {
        try {

            JsonNode updatedDoc = objectMapper.readTree(doc.toString());

            String gatewayPath = determineGatewayPath(serviceName);

            com.fasterxml.jackson.databind.node.ArrayNode servers;
            if (updatedDoc.has("servers")) {
                servers = (com.fasterxml.jackson.databind.node.ArrayNode) updatedDoc.get("servers");
                servers.removeAll();
            } else {
                servers = objectMapper.createArrayNode();
                ((com.fasterxml.jackson.databind.node.ObjectNode) updatedDoc).set("servers", servers);
            }

            com.fasterxml.jackson.databind.node.ObjectNode server = objectMapper.createObjectNode();
            server.put("url", gatewayServerUrl + gatewayPath);
            server.put("description", serviceName + " via API Gateway");
            servers.add(server);
            
            return updatedDoc;
        } catch (Exception e) {
            log.warn("Failed to update server URLs for service: {}", serviceName, e);
            return doc;
        }
    }
    
    private String determineGatewayPath(String serviceName) {
        try {
            List<RouteDefinition> routeDefinitions = routeDefinitionLocator.getRouteDefinitions()
                    .collectList()
                    .block(java.time.Duration.ofSeconds(2));
            
            if (routeDefinitions != null) {
                String serviceUri = "lb://" + serviceName.toLowerCase();

                for (RouteDefinition routeDefinition : routeDefinitions) {
                    String routeUri = routeDefinition.getUri().toString();

                    if (routeUri.equalsIgnoreCase(serviceUri) || routeUri.equals("lb://" + serviceName)) {
                        Optional<String> path = routeDefinition.getPredicates().stream()
                                .filter(p -> "Path".equals(p.getName()))
                                .map(p -> {
                                    Map<String, String> args = p.getArgs();
                                    if (args != null && !args.isEmpty()) {
                                        String pathValue = args.values().stream()
                                                .filter(v -> v != null && v.startsWith("/"))
                                                .findFirst()
                                                .orElse(null);
                                        
                                        if (pathValue != null) {
                                            return pathValue.replace("/**", "").replace("/*", "");
                                        }
                                    }
                                    return null;
                                })
                                .filter(Objects::nonNull)
                                .findFirst();
                        
                        if (path.isPresent()) {
                            log.debug("Found custom path for service {}: {}", serviceName, path.get());
                            return path.get();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to determine gateway path from route definitions for service: {}", serviceName, e);
        }

        log.debug("Using auto-discovered path for service {}: /{}", serviceName, serviceName.toLowerCase());
        return "/" + serviceName.toLowerCase();
    }
}
