package ru.domium.building.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.domium.security.util.SecurityUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/buildings")
@RequiredArgsConstructor
@Tag(name = "Building", description = "API для управления строительством домов")
public class BuildingController {

    @Operation(
            summary = "Публичный эндпоинт - информация о сервисе",
            description = "Возвращает публичную информацию о сервисе. Доступен без авторизации.",
            security = {}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешный ответ",
                    content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @GetMapping("/public/info")
    public ResponseEntity<Map<String, Object>> getPublicInfo() {
        log.info("Public info endpoint called");
        
        Map<String, Object> response = new HashMap<>();
        response.put("service", "Building Service");
        response.put("version", "1.0.0");
        response.put("description", "API для управления строительством домов");
        response.put("status", "active");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Тестовый endpoint",
            description = "Возвращает тестовую информацию о здании с данными пользователя"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешный ответ",
                    content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @GetMapping("/test")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getTest(
            @Parameter(description = "JWT токен текущего пользователя", hidden = true)
            @AuthenticationPrincipal Jwt jwt) {
        
        String userId = SecurityUtils.getCurrentUserId(jwt);
        @SuppressWarnings("unchecked")
        List<String> roles = jwt.getClaimAsMap("realm_access") != null
                ? (List<String>) jwt.getClaimAsMap("realm_access").getOrDefault("roles", List.of())
                : List.of();
        
        log.info("Test endpoint called. UserId: {}, Roles: {}", userId, roles);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Building service endpoint");
        response.put("userId", userId);
        response.put("roles", roles);
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Получить информацию о здании (для строителей)",
            description = "Возвращает информацию о здании по его ID. Доступно только для строителей."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Информация о здании найдена",
                    content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "404", description = "Здание не найдено")
    })
    @GetMapping("/builder/{id}")
    @PreAuthorize("hasRole('BUILDER')")
    public ResponseEntity<Map<String, Object>> getBuildingForBuilder(
            @Parameter(description = "ID здания", required = true, example = "123")
            @PathVariable String id,
            @Parameter(description = "JWT токен текущего пользователя", hidden = true)
            @AuthenticationPrincipal Jwt jwt) {
        
        String builderId = SecurityUtils.getCurrentUserId(jwt);
        
        log.info("Get building {} for builder. BuilderId: {}", id, builderId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("buildingId", id);
        response.put("builderId", builderId);
        response.put("name", "Test Building for Builder");
        response.put("status", "in_progress");
        
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Получить информацию о здании (для клиентов)",
            description = "Возвращает информацию о здании по его ID. Доступно только для клиентов."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Информация о здании найдена",
                    content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "404", description = "Здание не найдено")
    })
    @GetMapping("/customer/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Map<String, Object>> getBuildingForCustomer(
            @Parameter(description = "ID здания", required = true, example = "123")
            @PathVariable String id,
            @Parameter(description = "JWT токен текущего пользователя", hidden = true)
            @AuthenticationPrincipal Jwt jwt) {
        
        String customerId = SecurityUtils.getCurrentUserId(jwt);
        
        log.info("Get building {} for customer. CustomerId: {}", id, customerId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("buildingId", id);
        response.put("customerId", customerId);
        response.put("name", "Test Building for Customer");
        response.put("status", "active");
        
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Получить информацию о здании",
            description = "Возвращает информацию о здании по его ID. Доступно для аутентифицированных пользователей."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Информация о здании найдена",
                    content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Здание не найдено")
    })
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getBuilding(
            @Parameter(description = "ID здания", required = true, example = "123")
            @PathVariable String id,
            @Parameter(description = "JWT токен текущего пользователя", hidden = true)
            @AuthenticationPrincipal Jwt jwt) {
        
        String userId = SecurityUtils.getCurrentUserId(jwt);
        
        log.info("Get building {}. UserId: {}", id, userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("buildingId", id);
        response.put("userId", userId);
        response.put("name", "Test Building");
        response.put("status", "active");
        
        return ResponseEntity.ok(response);
    }
}

