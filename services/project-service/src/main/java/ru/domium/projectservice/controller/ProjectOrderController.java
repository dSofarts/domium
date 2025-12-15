package ru.domium.projectservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.domium.projectservice.entity.ProjectOrder;
import ru.domium.projectservice.service.ProjectOrderService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/project-orders")
@RequiredArgsConstructor
public class ProjectOrderController {

    private final ProjectOrderService projectOrderService;

    @GetMapping
    public ResponseEntity<List<ProjectOrder>> getAll() {
        return ResponseEntity.ok(projectOrderService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectOrder> getById(@PathVariable UUID id) {
        ProjectOrder order = projectOrderService.getById(id);
        if (order != null) {
            return ResponseEntity.ok(order);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<ProjectOrder> create(@RequestBody ProjectOrder order) {
        return ResponseEntity.ok(projectOrderService.create(order));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        projectOrderService.delete(id);
        return ResponseEntity.ok().build();
    }
}

