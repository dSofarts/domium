package ru.domium.building.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.domium.building.api.dto.video.CameraDto;
import ru.domium.building.api.dto.video.CreateCameraRequest;
import ru.domium.building.model.BuildingCamera;
import ru.domium.building.repository.BuildingCameraRepository;
import ru.domium.building.service.video.FfmpegStreamManager;
import ru.domium.building.service.video.VideoPaths;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class CameraService {
    private final BuildingCameraRepository cameraRepository;
    private final FfmpegStreamManager streamManager;

    @Transactional(readOnly = true)
    public List<CameraDto> list(UUID buildingId) {
        return cameraRepository.findByBuildingIdOrderByCreatedAtAsc(buildingId).stream()
                .map(c -> toDto(buildingId, c))
                .toList();
    }

    @Transactional(readOnly = true)
    public CameraDto get(UUID buildingId, UUID cameraId) {
        BuildingCamera cam = cameraRepository.findByIdAndBuildingId(cameraId, buildingId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Camera not found: " + cameraId));
        return toDto(buildingId, cam);
    }

    @Transactional
    public CameraDto create(UUID buildingId, CreateCameraRequest req) {
        if (req == null) throw new IllegalArgumentException("request is required");
        if (req.getName() == null || req.getName().isBlank()) throw new IllegalArgumentException("name is required");
        if (req.getRtspUrl() == null || req.getRtspUrl().isBlank()) throw new IllegalArgumentException("rtspUrl is required");

        BuildingCamera cam = new BuildingCamera();
        cam.setBuildingId(buildingId);
        cam.setName(req.getName().trim());
        cam.setRtspUrl(req.getRtspUrl().trim());
        cam.setEnabled(true);
        cam.setTranscode(Boolean.TRUE.equals(req.getTranscode()));
        cam.setCreatedAt(LocalDateTime.now());
        cam.setUpdatedAt(LocalDateTime.now());

        cam = cameraRepository.save(cam);
        return toDto(buildingId, cam);
    }

    @Transactional(readOnly = true)
    public BuildingCamera requireCamera(UUID buildingId, UUID cameraId) {
        return cameraRepository.findByIdAndBuildingId(cameraId, buildingId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Camera not found: " + cameraId));
    }

    public void ensureStarted(UUID buildingId, BuildingCamera camera) {
        streamManager.start(buildingId, camera);
    }

    public void stop(UUID cameraId) {
        streamManager.stop(cameraId);
    }

    private CameraDto toDto(UUID buildingId, BuildingCamera c) {
        CameraDto dto = new CameraDto();
        dto.setId(c.getId());
        dto.setBuildingId(buildingId);
        dto.setName(c.getName());
        dto.setEnabled(c.isEnabled());
        dto.setTranscode(c.isTranscode());
        dto.setHlsUrl(VideoPaths.hlsUrl(buildingId, c.getId()));
        dto.setRunning(streamManager.isRunning(c.getId()));
        return dto;
    }
}


