package ru.domium.building.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import ru.domium.building.repository.BuildingCameraRepository;
import ru.domium.building.service.AccessService;
import ru.domium.building.service.video.FfmpegStreamManager;
import ru.domium.security.util.SecurityUtils;

import java.util.UUID;

import static org.springframework.http.HttpStatus.FORBIDDEN;

/**
 * Endpoint для nginx auth_request.
 */
@RestController
@RequiredArgsConstructor
public class AuthController {
    private final AccessService accessService;
    private final BuildingCameraRepository cameraRepository;
    private final FfmpegStreamManager streamManager;

    @GetMapping("/internal/video/auth")
    public ResponseEntity<Void> auth(@RequestParam UUID buildingId,
                                     @RequestParam UUID cameraId,
                                     @AuthenticationPrincipal Jwt jwt) {
        UUID userId = SecurityUtils.requireSubjectUuid(jwt);
        accessService.requireAccess(buildingId, userId);
        if (!cameraRepository.existsByIdAndBuildingId(cameraId, buildingId)) {
            throw new ResponseStatusException(FORBIDDEN, "Camera not accessible");
        }
        streamManager.touch(cameraId);
        return ResponseEntity.noContent().build();
    }
}


