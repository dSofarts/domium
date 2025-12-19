package ru.domium.projectservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.domium.projectservice.dto.response.ProjectOrderResponse;
import ru.domium.projectservice.dto.response.ProjectResponse;
import ru.domium.projectservice.entity.ProjectOrder;
import ru.domium.projectservice.service.ProjectOrderService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/projects/{projectId}/orders")
@RequiredArgsConstructor
public class ProjectOrderController {

    private final ProjectOrderService projectOrderService;

    //    @PreAuthorize("hasRole('USER')")
    @PostMapping
    public ResponseEntity<ProjectOrderResponse> placeOrder(@PathVariable UUID projectId) {
        ProjectOrderResponse createdOrder = projectOrderService.createOrder(projectId);
        return ResponseEntity.ok(createdOrder);
    }

    //    @PreAuthorize("hasRole('USER')")
    @GetMapping("/my")
    public ResponseEntity<List<ProjectResponse>> getPersonal(@PathVariable UUID projectId,
                                                             @RequestParam UUID userId) {
//        TODO: Correct implementation
        return ResponseEntity.ok(List.of());
    }

    //    @PreAuthorize("hasRole('OWNER')")
    @GetMapping
    public ResponseEntity<List<ProjectOrder>> getAllOrders(@PathVariable UUID projectId) {
        // TODO: Correct implementation
        List<ProjectOrder> orders = projectOrderService.getAll();
        return ResponseEntity.ok(orders);
    }

    //    @PreAuthorize("hasRole('USER')")
    @GetMapping("/my/{orderId}")
    public ResponseEntity<ProjectOrder> getPersonalOrderById(@PathVariable UUID projectId,
                                                     @PathVariable UUID orderId) {
        // TODO: Correct implementation
        ProjectOrder order = projectOrderService.getById(orderId);
        if (order != null) {
            return ResponseEntity.ok(order);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    //    @PreAuthorize("hasRole('OWNER')")
    @GetMapping("/{orderId}")
    public ResponseEntity<ProjectOrder> getOrderById(@PathVariable UUID projectId,
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

