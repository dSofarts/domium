package ru.domium.projectservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.domium.projectservice.dto.response.ProjectOrderResponse;
import ru.domium.projectservice.entity.ProjectOrder;
import ru.domium.projectservice.service.ProjectOrderService;
import ru.domium.security.util.SecurityUtils;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Project Orders", description = "API для управления заказами на проекты")
public class ProjectOrderController {

    private final ProjectOrderService projectOrderService;

    @Operation(
            summary = "Создать заказ на проект",
            description = "Создаёт заказ на указанный проект. Доступно для роли CLIENT."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Заказ успешно создан",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProjectOrderResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Проект не найден")
    })
    @PreAuthorize("hasRole('CLIENT')")
    @PostMapping("{projectId}/orders")
    public ResponseEntity<ProjectOrderResponse> placeOrder(
            @Parameter(description = "ID проекта", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID projectId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID clientId = SecurityUtils.requireSubjectUuid(jwt);
        ProjectOrderResponse createdOrder = projectOrderService.createOrder(projectId, clientId);

        var location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/my/{orderId}")
                .buildAndExpand(createdOrder.getId())
                .toUri();

        return ResponseEntity.created(location).body(createdOrder);
    }

    @Operation(
            summary = "Получить свои заказы по проекту",
            description = "Возвращает список заказов пользователя по проекту. Метод пока не реализован."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список заказов пользователя по проекту",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ProjectOrderResponse.class))
                    )
            ),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав")
    })
    @PreAuthorize("hasRole('CLIENT')")
    @GetMapping("/orders/my")
    public ResponseEntity<List<ProjectOrderResponse>> getPersonalOrders(
            @Parameter(description = "ID пользователя", required = true)
            @AuthenticationPrincipal Jwt jwt) {
        UUID clientId = SecurityUtils.requireSubjectUuid(jwt);
        return ResponseEntity.ok(projectOrderService.getAllByClientId(clientId));
    }

    @Operation(
            summary = "Получить свой заказ по ID",
            description = "Возвращает заказ пользователя по ID. Метод временно реализован частично и может измениться."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Заказ найден",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProjectOrderResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Заказ не найден")
    })
    @PreAuthorize("hasRole('CLIENT')")
    @GetMapping("/orders/my/{orderId}")
    public ResponseEntity<ProjectOrderResponse> getPersonalOrderById(
            @Parameter(description = "ID заказа пользователя", required = true)
            @PathVariable UUID orderId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID clientId = SecurityUtils.requireSubjectUuid(jwt);
        return projectOrderService.getByIdAndClientId(orderId, clientId);
    }

    @Operation(
            summary = "Получить все заказы по проекту",
            description = "Возвращает все заказы по проекту для менеджера. Метод временно возвращает все заказы без фильтрации по проекту."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список заказов по проекту",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ProjectOrderResponse.class))
                    )
            ),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав")
    })
    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/orders")
    public ResponseEntity<List<ProjectOrderResponse>> getAllOrders(@AuthenticationPrincipal Jwt jwt) {
        UUID managerId = SecurityUtils.requireSubjectUuid(jwt);
        return ResponseEntity.ok(projectOrderService.getAllByManagerId(managerId));
    }

    @Operation(
            summary = "Получить заказ по ID",
            description = "Возвращает заказ по ID для менеджера. Метод временно не учитывает projectId."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Заказ найден",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProjectOrder.class)
                    )
            ),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Заказ не найден")
    })
    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("orders/{orderId}")
    public ResponseEntity<ProjectOrderResponse> getOrderById(
            @Parameter(description = "ID заказа", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID orderId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID managerId = SecurityUtils.requireSubjectUuid(jwt);
        ProjectOrderResponse order = projectOrderService.getByIdAndManagerId(orderId, managerId);
        return ResponseEntity.ok(order);
    }

    @Operation(
            summary = "Получить заказы по проекту",
            description = "Возвращает список заказов по указанному projectId для менеджера-владельца проекта."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список заказов по проекту",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ProjectOrderResponse.class))
                    )
            ),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Проект не найден")
    })
    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("{projectId}/orders")
    public ResponseEntity<List<ProjectOrderResponse>> getOrdersByProjectId(
            @Parameter(description = "ID проекта", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID projectId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID managerId = SecurityUtils.requireSubjectUuid(jwt);
        return ResponseEntity.ok(projectOrderService.getAllByProjectIdAndManagerId(projectId, managerId));
    }
}
