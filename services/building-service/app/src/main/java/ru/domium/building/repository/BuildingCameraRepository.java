package ru.domium.building.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.domium.building.model.BuildingCamera;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BuildingCameraRepository extends JpaRepository<BuildingCamera, UUID> {
    List<BuildingCamera> findByBuildingIdOrderByCreatedAtAsc(UUID buildingId);

    Optional<BuildingCamera> findByIdAndBuildingId(UUID id, UUID buildingId);

    boolean existsByIdAndBuildingId(UUID id, UUID buildingId);
}


