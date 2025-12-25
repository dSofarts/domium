package ru.domium.projectservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.domium.projectservice.entity.ProjectImage;

import java.util.UUID;

@Repository
public interface ProjectImageRepository extends JpaRepository<ProjectImage, UUID> {
}

