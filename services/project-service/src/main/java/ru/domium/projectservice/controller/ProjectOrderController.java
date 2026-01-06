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
import org.springframework.web.bind.annotation.*;
import ru.domium.projectservice.dto.response.ProjectOrderResponse;
import ru.domium.projectservice.dto.response.ProjectResponse;
import ru.domium.projectservice.entity.ProjectOrder;
import ru.domium.projectservice.service.ProjectOrderService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("{projectId}/orders")
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
    @PostMapping
    public ResponseEntity<ProjectOrderResponse> placeOrder(
            @Parameter(description = "ID проекта", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID projectId) {
        ProjectOrderResponse createdOrder = projectOrderService.createOrder(projectId);
        return ResponseEntity.ok(createdOrder);
    }

    @Deprecated
    @Operation(
            summary = "Получить свои заказы по проекту",
            description = "Возвращает список заказов пользователя по проекту. Метод пока не реализован.",
            deprecated = true
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список заказов пользователя по проекту",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ProjectResponse.class))
                    )
            ),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав")
    })
    @PreAuthorize("hasRole('CLIENT')")
    @GetMapping("/my")
    public ResponseEntity<List<ProjectResponse>> getPersonalOrders(
            @Parameter(description = "ID проекта", required = true)
            @PathVariable UUID projectId,
            @Parameter(description = "ID пользователя", required = true)
            @RequestParam UUID userId) {
//        TODO: Correct implementation
        return ResponseEntity.ok(List.of());
    }

    @Deprecated
    @Operation(
            summary = "Получить свой заказ по ID",
            description = "Возвращает заказ пользователя по ID. Метод временно реализован частично и может измениться.",
            deprecated = true
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
    @PreAuthorize("hasRole('CLIENT')")
    @GetMapping("/my/{orderId}")
    public ResponseEntity<ProjectOrder> getPersonalOrderById(
            @Parameter(description = "ID проекта", required = true)
            @PathVariable UUID projectId,
            @Parameter(description = "ID заказа пользователя", required = true)
            @PathVariable UUID orderId) {
        // TODO: Correct implementation
        ProjectOrder order = projectOrderService.getById(orderId);
        if (order != null) {
            return ResponseEntity.ok(order);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Deprecated
    @Operation(
            summary = "Получить все заказы по проекту",
            description = "Возвращает все заказы по проекту для менеджера. Метод временно возвращает все заказы без фильтрации по проекту.",
            deprecated = true
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список заказов по проекту",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ProjectOrder.class))
                    )
            ),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав")
    })
    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping
    public ResponseEntity<List<ProjectOrder>> getAllOrders(
            @Parameter(description = "ID проекта", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID projectId) {
        // TODO: Correct implementation
        List<ProjectOrder> orders = projectOrderService.getAll();
        return ResponseEntity.ok(orders);
    }

    @Deprecated
    @Operation(
            summary = "Получить заказ по ID",
            description = "Возвращает заказ по ID для менеджера. Метод временно не учитывает projectId.",
            deprecated = true
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
    @GetMapping("/{orderId}")
    public ResponseEntity<ProjectOrder> getOrderById(
            @Parameter(description = "ID проекта", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID projectId,
            @Parameter(description = "ID заказа", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID orderId) {
        // TODO: Correct implementation
        ProjectOrder order = projectOrderService.getById(orderId);
        if (order != null) {
            return ResponseEntity.ok(order);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

