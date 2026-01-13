package ru.domium.projectservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.domium.projectservice.entity.Project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    @Query("select p from Project p left join fetch p.images where p.id = :projectId")
    Optional<Project> findByIdWithImages(@Param("projectId") UUID projectId);

    List<Project> findAllByManagerUserId(UUID managerUserId);
}

