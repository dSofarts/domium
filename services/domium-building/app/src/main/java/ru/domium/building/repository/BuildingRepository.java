package ru.domium.building.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.domium.building.model.Building;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface BuildingRepository extends JpaRepository<Building, UUID> {
    List<Building> findByClientId(UUID clientId);

    boolean existsByWorkflowIdAndCurrentStageIdIn(UUID workflowId, Collection<UUID> stageIds);
}
