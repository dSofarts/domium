package ru.domium.building.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ru.domium.building.mapper.ProjectMapper;
import ru.domium.building.mapper.TransitionMapper;
import ru.domium.security.util.SecurityUtils;
import ru.domium.security.annotation.PublicEndpoint;
import ru.domium.building.service.BuildingService;
import ru.domium.building.api.dto.BuildingDetailsDto;
import ru.domium.building.api.dto.CreateBuildingRequest;
import ru.domium.building.api.dto.project.ProjectDto;
import ru.domium.building.api.dto.substage.SubStageStatusDto;
import ru.domium.building.api.dto.transition.StageTransitionDeniedDto;
import ru.domium.building.api.dto.workflow.StageDto;
import ru.domium.building.repository.BuildingProjectionRepository;
import ru.domium.building.model.BuildingProjection;
import ru.domium.building.service.stage.requirement.StageTransitionNotAllowedException;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Building", description = "API для управления строительством домов")
public class BuildingController {

    private final BuildingService buildingService;
    private final BuildingProjectionRepository buildingProjectionRepository;
    private final ProjectMapper projectMapper;
    private final TransitionMapper transitionMapper;

    @Operation(summary = "Инициация стройки")
    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ProjectDto> create(@AuthenticationPrincipal Jwt jwt,
                                              @RequestBody CreateBuildingRequest request) {
        UUID clientId = SecurityUtils.requireSubjectUuid(jwt);
        if (request.getProject() == null) throw new IllegalArgumentException("project is required");
        if (request.getProject().getId() == null) throw new IllegalArgumentException("project.id is required");
        if (request.getProject().getManager() == null || request.getProject().getManager().getId() == null) {
            throw new IllegalArgumentException("project.manager.id is required");
        }
        String projectName = request.getProject().getObjectInfo() != null ? request.getProject().getObjectInfo().getProjectName() : null;
        String managerName = request.getProject().getManager() != null ? request.getProject().getManager().getName() : null;
        var attributes = request.getProject().getObjectInfo() != null ? request.getProject().getObjectInfo().getAttributes() : null;

        BuildingProjection projection = buildingService.createBuilding(
                request.getProject().getId(),
                projectName,
                clientId,
                request.getProject().getManager().getId(),
                managerName,
                attributes,
                request.getWorkflowId()
        );
        return ResponseEntity.ok(projectMapper.toProjectDto(projection));
    }

    @Operation(summary = "Список строек текущего пользователя (CLIENT/MANAGER)")
    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENT','MANAGER')")
    public ResponseEntity<List<ProjectDto>> getMyBuildings(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = SecurityUtils.requireSubjectUuid(jwt);

        if (SecurityUtils.hasRoleIgnoreCase(jwt, "MANAGER")) {
            String managerNameFromJwt = SecurityUtils.resolveDisplayName(jwt);
            List<BuildingProjection> projections = buildingProjectionRepository.findByManagerId(userId);

            if (managerNameFromJwt != null && !managerNameFromJwt.isBlank()) {
                boolean changed = false;
                for (BuildingProjection p : projections) {
                    if (p.getManagerName() == null || p.getManagerName().isBlank()) {
                        p.setManagerName(managerNameFromJwt);
                        changed = true;
                    }
                }
                if (changed) buildingProjectionRepository.saveAll(projections);
            }

            return ResponseEntity.ok(projections.stream().map(projectMapper::toProjectDto).toList());
        }

        return ResponseEntity.ok(buildingProjectionRepository.findByClientId(userId).stream().map(projectMapper::toProjectDto).toList());
    }

    @Operation(summary = "Получить стройку по ID")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BuildingDetailsDto> getBuilding(@PathVariable UUID id,
                                                          @AuthenticationPrincipal Jwt jwt) {
        UUID userId = SecurityUtils.requireSubjectUuid(jwt);
        BuildingDetailsDto dto = new BuildingDetailsDto();
        dto.setBuilding(buildingService.getBuilding(id, userId));
        dto.setProject(buildingProjectionRepository.findById(id).map(projectMapper::toProjectDto).orElse(null));
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Список этапов (stages) по workflow этой стройки")
    @GetMapping("/{id}/stages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<StageDto>> getStages(@PathVariable UUID id,
                                                    @AuthenticationPrincipal Jwt jwt) {
        UUID userId = SecurityUtils.requireSubjectUuid(jwt);
        return ResponseEntity.ok(buildingService.getStages(id, userId));
    }

    @Operation(summary = "Список подэтапов (substage) указанной стадии стройки с флагом завершения")
    @GetMapping("/{buildingId}/stages/{stageId}/substages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SubStageStatusDto>> getStageSubStages(@PathVariable UUID buildingId,
                                                                     @PathVariable UUID stageId,
                                                                       @AuthenticationPrincipal Jwt jwt) {
        UUID userId = SecurityUtils.requireSubjectUuid(jwt);
        return ResponseEntity.ok(buildingService.getStageSubStages(buildingId, stageId, userId));
    }

    @Operation(summary = "Отметить вид работ (work item) текущей стадии как выполненный")
    @PostMapping("/{id}/work-items/{workItemId}/complete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> completeWorkItem(@PathVariable UUID id,
                                                 @PathVariable UUID workItemId,
                                                 @AuthenticationPrincipal Jwt jwt) {
        UUID userId = SecurityUtils.requireSubjectUuid(jwt);
        buildingService.completeWorkItem(id, userId, workItemId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Перейти на следующий этап")
    @PostMapping("/{id}/stage/next")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> nextStage(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = SecurityUtils.requireSubjectUuid(jwt);
        try {
            String managerNameFromJwt = SecurityUtils.hasRoleIgnoreCase(jwt, "MANAGER") ? SecurityUtils.resolveDisplayName(jwt) : null;
            BuildingProjection projection = buildingService.nextStage(id, userId, managerNameFromJwt);
            return ResponseEntity.ok(projectMapper.toProjectDto(projection));
        } catch (StageTransitionNotAllowedException e) {
            StageTransitionDeniedDto denied = transitionMapper.toDenied(id, e.getViolations());
            return ResponseEntity.status(409).body(denied);
        }
    }

    @Operation(summary = "Видео стройки (demo)")
    @GetMapping("/{id}/video")
    @PublicEndpoint
    public String videoStream(@PathVariable UUID id) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="margin:0;background:black;display:flex;align-items:center;justify-content:center;height:100vh">
                <video width="100%" height="100%" controls autoplay muted>
                    <source src="https://sample-videos.com/zip/10/mp4/SampleVideo_1280x720_1mb.mp4" type="video/mp4">
                </video>
                <div style="position:absolute;bottom:20px;left:20px;background:rgba(0,0,0,0.7);color:white;padding:10px;border-radius:5px">
                    🟢 LIVE: Строительная площадка
                </div>
            </body>
            </html>
            """;
    }

}
