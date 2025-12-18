package ru.domium.projectservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.domium.projectservice.dto.request.CreateProjectRequest;
import ru.domium.projectservice.dto.response.ProjectResponse;
import ru.domium.projectservice.service.ProjectService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    //    @PreAuthorize("hasRole('OWNER')")
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@RequestBody @Valid CreateProjectRequest request) {
        ProjectResponse created = projectService.createProject(request);
        URI location = URI.create("/projects/" + created.getId());
        return ResponseEntity.created(location).
                body(created);
    }


    //    @PreAuthorize("hasRole('OWNER')")
    @PutMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> updateProject(@PathVariable String projectId,
                                                         @RequestBody @Valid CreateProjectRequest request) {
        //TODO: Implementation for updating a project would go here
        return ResponseEntity.notFound().build(); // Placeholder response
    }


    //    @PreAuthorize("hasRole('OWNER')")
    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(@PathVariable String projectId) {
        //TODO: Deleting a project implementation would go here
        return ResponseEntity.notFound().build(); // Placeholder response
    }


    //    @PreAuthorize("hasRole('USER')")
    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAll() {
        return ResponseEntity.ok(projectService.getAllProjects());
    }


    //    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{projectId}/like")
    public ResponseEntity<Void> likeProject(@PathVariable String projectId) {
//        TODO: Implementation for liking a project would go here
        return ResponseEntity.notFound().build(); // Placeholder response
    }


    //    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{projectId}/unlike")
    public ResponseEntity<Void> unlikeProject(@PathVariable String projectId) {
//        TODO: Implementation for unliking a project would go here
        return ResponseEntity.notFound().build(); // Placeholder response
    }
}
