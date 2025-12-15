package ru.domium.projectservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.domium.projectservice.entity.Project;
import ru.domium.projectservice.repository.ProjectRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }
}

