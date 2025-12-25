package ru.domium.projectservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.domium.projectservice.dto.request.CreateProjectRequest;
import ru.domium.projectservice.dto.response.ProjectResponse;
import ru.domium.projectservice.service.ProjectService;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@RequestBody @Valid CreateProjectRequest request) {
        ProjectResponse created = projectService.createProject(request);
        URI location = URI.create("/projects/" + created.getId());
        return ResponseEntity.created(location).
                body(created);
    }


    @PreAuthorize("hasRole('MANAGER')")
    @PutMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> updateProject(@PathVariable String projectId,
                                                         @RequestBody @Valid CreateProjectRequest request) {
        //TODO: Implementation for updating a project would go here
        return ResponseEntity.notFound().build(); // Placeholder response
    }


    @PreAuthorize("hasRole('MANAGER')")
    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(@PathVariable String projectId) {
        //TODO: Deleting a project implementation would go here
        return ResponseEntity.notFound().build(); // Placeholder response
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAllProjects() {
        return ResponseEntity.ok(projectService.getAllProjects());
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable UUID projectId) {
        ProjectResponse project = projectService.getProjectById(projectId);
        return ResponseEntity.ok(project);
    }

    @Deprecated(forRemoval = false)
    @PreAuthorize("hasRole('CLIENT')")
    @PostMapping("/{projectId}/like")
    public ResponseEntity<Void> likeProject(@PathVariable String projectId) {
//        TODO: Implementation for liking a project would go here
        return ResponseEntity.notFound().build(); // Placeholder response
    }

    @Deprecated(forRemoval = false)
    @PreAuthorize("hasRole('CLIENT')")
    @PostMapping("/{projectId}/unlike")
    public ResponseEntity<Void> unlikeProject(@PathVariable String projectId) {
//        TODO: Implementation for unliking a project would go here
        return ResponseEntity.notFound().build(); // Placeholder response
    }
}
