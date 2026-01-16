package ru.domium.building.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.domium.building.model.Stage;

import java.util.List;
import java.util.UUID;

public interface StageRepository extends JpaRepository<Stage, UUID> {
    List<Stage> findByWorkflowIdOrderByPosition(UUID workflowId);

    List<Stage> findByWorkflowId(UUID workflowId);
}
