package ru.domium.building.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.domium.building.model.Task;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findByStageIdAndParentIdIsNullOrderByPosition(UUID stageId);
    List<Task> findByParentIdOrderByPosition(UUID parentId);
    List<Task> findByParentIdInOrderByParentIdAscPositionAsc(Collection<UUID> parentIds);
}
