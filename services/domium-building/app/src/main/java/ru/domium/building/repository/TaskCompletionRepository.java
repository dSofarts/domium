package ru.domium.building.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.domium.building.model.TaskCompletion;
import ru.domium.building.model.TaskCompletionId;

import java.util.Set;
import java.util.UUID;

public interface TaskCompletionRepository extends JpaRepository<TaskCompletion, TaskCompletionId> {
    @Query("select c.id.taskId from TaskCompletion c where c.id.buildingId = :buildingId and c.id.taskId in :taskIds")
    Set<UUID> findCompletedTaskIds(@Param("buildingId") UUID buildingId, @Param("taskIds") Set<UUID> taskIds);
}
