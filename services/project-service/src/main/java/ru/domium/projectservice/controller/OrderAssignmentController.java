package ru.domium.projectservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.domium.projectservice.entity.OrderAssignment;
import ru.domium.projectservice.service.OrderAssignmentService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/order-assignments")
@RequiredArgsConstructor
public class OrderAssignmentController {

    private final OrderAssignmentService orderAssignmentService;

    @GetMapping
    public ResponseEntity<List<OrderAssignment>> getAll() {
        return ResponseEntity.ok(orderAssignmentService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderAssignment> getById(@PathVariable UUID id) {
        OrderAssignment orderAssignment = orderAssignmentService.getById(id);
        if (orderAssignment != null) {
            return ResponseEntity.ok(orderAssignment);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<OrderAssignment> create(@RequestBody OrderAssignment orderAssignment) {
        return ResponseEntity.ok(orderAssignmentService.create(orderAssignment));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        orderAssignmentService.delete(id);
        return ResponseEntity.ok().build();
    }
}

