package ru.domium.building.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.domium.building.model.Workflow;

import java.util.Optional;
import java.util.UUID;

public interface WorkflowRepository extends JpaRepository<Workflow, UUID> {
    Optional<Workflow> findByManagerIdAndActiveTrue(UUID managerId);

    @Modifying
    @Query("update Workflow w set w.active=false where w.managerId = :managerId and w.active=true")
    int deactivateActive(@Param("managerId") UUID managerId);
}
