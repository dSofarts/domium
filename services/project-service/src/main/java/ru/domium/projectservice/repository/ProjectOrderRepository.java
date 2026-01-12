package ru.domium.projectservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.domium.projectservice.entity.Project;
import ru.domium.projectservice.entity.ProjectOrder;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectOrderRepository extends JpaRepository<ProjectOrder, UUID> {
    List<ProjectOrder> findAllByClientUserId(UUID clientUserId);

    Optional<ProjectOrder> findByIdAndClientUserId(UUID id, UUID clientUserId);

    List<ProjectOrder> findAllByProjectIn(Collection<Project> projects);

    List<ProjectOrder> findAllByProject_Id(UUID projectId);

    List<ProjectOrder> findAllByProject_IdAndProject_ManagerUserId(UUID projectId, UUID managerUserId);
}
