package ru.domium.building.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.domium.building.model.BuildingProjection;

import java.util.List;
import java.util.UUID;

public interface BuildingProjectionRepository extends JpaRepository<BuildingProjection, UUID> {
    List<BuildingProjection> findByClientId(UUID clientId);

    List<BuildingProjection> findByManagerId(UUID managerId);
}
