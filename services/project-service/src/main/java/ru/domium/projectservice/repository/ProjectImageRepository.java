package ru.domium.projectservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.domium.projectservice.entity.ProjectImage;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectImageRepository extends JpaRepository<ProjectImage, UUID> {

    List<ProjectImage> findAllByIdInAndProject_Id(Collection<UUID> ids, UUID projectId);

    Optional<ProjectImage> findByIdAndProject_Id(UUID id, UUID projectId);
}

