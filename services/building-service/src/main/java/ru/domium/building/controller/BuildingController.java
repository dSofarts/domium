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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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
    public ResponseEntity<Map<String, Object>> getTest(
            @Parameter(description = "ID пользователя из заголовка X-User-Id", hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(description = "Роли пользователя из заголовка X-User-Roles", hidden = true)
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        
        String userRole = extractRole(roles);
        log.info("Test endpoint called. UserId: {}, Roles: {}, UserRole: {}", userId, roles, userRole);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Building service - " + userRole + " endpoint");
        response.put("userId", userId);
        response.put("roles", roles);
        response.put("userRole", userRole);
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Получить информацию о здании",
            description = "Возвращает информацию о здании по его ID. Данные зависят от роли пользователя."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Информация о здании найдена",
                    content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Здание не найдено")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getBuilding(
            @Parameter(description = "ID здания", required = true, example = "123")
            @PathVariable String id,
            @Parameter(description = "ID пользователя из заголовка X-User-Id", hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(description = "Роли пользователя из заголовка X-User-Roles", hidden = true)
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        
        String userRole = extractRole(roles);
        log.info("Get building {}. UserId: {}, Roles: {}, UserRole: {}", id, userId, roles, userRole);
        
        Map<String, Object> response = new HashMap<>();
        response.put("buildingId", id);
        response.put("userRole", userRole);
        response.put("userId", userId);
        response.put("roles", roles);
        
        if ("CLIENT".equals(userRole)) {
            response.put("name", "Test Building for Client");
            response.put("status", "active");
        } else if ("BUILDER".equals(userRole)) {
            response.put("name", "Test Building for Builder");
            response.put("status", "in_progress");
        } else {
            response.put("name", "Test Building");
            response.put("status", "unknown");
        }
        
        return ResponseEntity.ok(response);
    }

    private String extractRole(String roles) {
        if (roles == null || roles.isEmpty()) {
            return "UNKNOWN";
        }
        
        if (roles.contains("ROLE_CLIENT")) {
            return "CLIENT";
        } else if (roles.contains("ROLE_BUILDER")) {
            return "BUILDER";
        }
        
        return "UNKNOWN";
    }
}

