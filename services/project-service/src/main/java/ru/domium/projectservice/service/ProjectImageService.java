package ru.domium.projectservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.domium.projectservice.entity.ProjectImage;
import ru.domium.projectservice.repository.ProjectImageRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectImageService {

    private final ProjectImageRepository projectImageRepository;

    public List<ProjectImage> getAll() {
        return projectImageRepository.findAll();
    }

    public ProjectImage getById(UUID id) {
        return projectImageRepository.findById(id).orElse(null);
    }

    public ProjectImage create(ProjectImage projectImage) {
        return projectImageRepository.save(projectImage);
    }

    public void delete(UUID id) {
        projectImageRepository.deleteById(id);
    }
}

