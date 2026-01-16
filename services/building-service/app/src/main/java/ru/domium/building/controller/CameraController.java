package ru.domium.building.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ru.domium.building.api.dto.video.CameraDto;
import ru.domium.building.api.dto.video.CreateCameraRequest;
import ru.domium.building.model.BuildingCamera;
import ru.domium.building.service.AccessService;
import ru.domium.building.service.CameraService;
import ru.domium.security.util.SecurityUtils;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("{buildingId}/cameras")
@RequiredArgsConstructor
@Tag(name = "Cameras", description = "IP-камеры и трансляции для стройки")
public class CameraController {
    private final AccessService accessService;
    private final CameraService cameraService;

    @Operation(summary = "Список камер стройки")
    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENT','MANAGER')")
    public ResponseEntity<List<CameraDto>> list(@PathVariable UUID buildingId,
                                                @AuthenticationPrincipal Jwt jwt) {
        UUID userId = SecurityUtils.requireSubjectUuid(jwt);
        accessService.requireAccess(buildingId, userId);
        return ResponseEntity.ok(cameraService.list(buildingId));
    }

    @Operation(summary = "Создать камеру (MANAGER)")
    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<CameraDto> create(@PathVariable UUID buildingId,
                                            @AuthenticationPrincipal Jwt jwt,
                                            @RequestBody CreateCameraRequest req) {
        UUID userId = SecurityUtils.requireSubjectUuid(jwt);
        accessService.requireAccess(buildingId, userId);
        return ResponseEntity.ok(cameraService.create(buildingId, req));
    }

    @Operation(summary = "Получить камеру")
    @GetMapping("/{cameraId}")
    @PreAuthorize("hasAnyRole('CLIENT','MANAGER')")
    public ResponseEntity<CameraDto> get(@PathVariable UUID buildingId,
                                         @PathVariable UUID cameraId,
                                         @AuthenticationPrincipal Jwt jwt) {
        UUID userId = SecurityUtils.requireSubjectUuid(jwt);
        accessService.requireAccess(buildingId, userId);
        return ResponseEntity.ok(cameraService.get(buildingId, cameraId));
    }

    @Operation(summary = "Получить URL трансляции (m3u8). Стартует поток по требованию.")
    @GetMapping("/{cameraId}/stream")
    @PreAuthorize("hasAnyRole('CLIENT','MANAGER')")
    public ResponseEntity<CameraDto> stream(@PathVariable UUID buildingId,
                                            @PathVariable UUID cameraId,
                                            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = SecurityUtils.requireSubjectUuid(jwt);
        accessService.requireAccess(buildingId, userId);
        BuildingCamera cam = cameraService.requireCamera(buildingId, cameraId);
        cameraService.ensureStarted(buildingId, cam);
        return ResponseEntity.ok(cameraService.get(buildingId, cameraId));
    }

    @Operation(summary = "Остановить поток камеры (MANAGER)")
    @PostMapping("/{cameraId}/stop")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> stop(@PathVariable UUID buildingId,
                                     @PathVariable UUID cameraId,
                                     @AuthenticationPrincipal Jwt jwt) {
        UUID userId = SecurityUtils.requireSubjectUuid(jwt);
        accessService.requireAccess(buildingId, userId);
        cameraService.requireCamera(buildingId, cameraId);
        cameraService.stop(cameraId);
        return ResponseEntity.ok().build();
    }
}


