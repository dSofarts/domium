package ru.domium.building.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.domium.building.model.Building;
import ru.domium.building.repository.BuildingRepository;

import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class AccessService {
    private final BuildingRepository buildingRepository;

    @Transactional(readOnly = true)
    public Building requireAccess(UUID buildingId, UUID userId) {
        if (userId == null) throw new AccessDeniedException("Unauthorized");
        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Building not found: " + buildingId));
        if (userId.equals(building.getClientId())) return building;
        if (userId.equals(building.getManagerId())) return building;
        throw new AccessDeniedException("Forbidden");
    }
}


