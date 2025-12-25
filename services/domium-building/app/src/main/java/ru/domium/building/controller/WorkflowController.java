package ru.domium.building.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ru.domium.security.util.SecurityUtils;
import ru.domium.building.service.WorkflowService;
import ru.domium.building.api.dto.workflow.CreateWorkflowRequest;
import ru.domium.building.api.dto.workflow.UpsertStagesRequest;
import ru.domium.building.api.dto.workflow.UpsertStageTasksRequest;
import ru.domium.building.api.dto.workflow.WorkflowDto;

import java.util.UUID;

@RestController
@RequestMapping("/workflows")
@RequiredArgsConstructor
@Tag(name = "Workflows", description = "Управление workflow этапов строительства (для MANAGER)")
@PreAuthorize("hasRole('MANAGER')")
public class WorkflowController {
    private final WorkflowService workflowService;

    @Operation(summary = "Активный workflow текущего manager")
    @GetMapping("/me/active")
    public ResponseEntity<WorkflowDto> getActive(@AuthenticationPrincipal Jwt jwt) {
        UUID managerId = SecurityUtils.requireSubjectUuid(jwt);
        return ResponseEntity.ok(workflowService.getActive(managerId));
    }

    @Operation(summary = "Получить workflow по ID")
    @GetMapping("/{id}")
    public ResponseEntity<WorkflowDto> get(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        UUID managerId = SecurityUtils.requireSubjectUuid(jwt);
        return ResponseEntity.ok(workflowService.get(id, managerId));
    }

    @Operation(summary = "Создать workflow")
    @PostMapping
    public ResponseEntity<WorkflowDto> create(@AuthenticationPrincipal Jwt jwt,
                                              @RequestBody CreateWorkflowRequest request) {
        UUID managerId = SecurityUtils.requireSubjectUuid(jwt);
        return ResponseEntity.ok(workflowService.create(managerId, request));
    }

    @Operation(summary = "Активировать workflow")
    @PostMapping("/{id}/activate")
    public ResponseEntity<WorkflowDto> activate(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        UUID managerId = SecurityUtils.requireSubjectUuid(jwt);
        return ResponseEntity.ok(workflowService.activate(id, managerId));
    }

    @Operation(summary = "Обновить список стадий workflow")
    @PutMapping("/{id}/stages")
    public ResponseEntity<WorkflowDto> upsertStages(@PathVariable UUID id,
                                                    @AuthenticationPrincipal Jwt jwt,
                                                    @RequestBody UpsertStagesRequest request) {
        UUID managerId = SecurityUtils.requireSubjectUuid(jwt);
        return ResponseEntity.ok(workflowService.upsertStages(id, managerId, request));
    }

    @Operation(summary = "Обновить дерево задач (subStages + workItems) для стадии workflow")
    @PutMapping("/{id}/stages/{stageId}/tasks")
    public ResponseEntity<WorkflowDto> upsertStageTasks(@PathVariable UUID id,
                                                        @PathVariable UUID stageId,
                                                        @AuthenticationPrincipal Jwt jwt,
                                                        @RequestBody UpsertStageTasksRequest request) {
        UUID managerId = SecurityUtils.requireSubjectUuid(jwt);
        return ResponseEntity.ok(workflowService.upsertStageTasks(id, stageId, managerId, request));
    }
}
