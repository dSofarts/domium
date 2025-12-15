package ru.domium.projectservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.domium.projectservice.entity.ProjectImage;
import ru.domium.projectservice.service.ProjectImageService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/project-images")
@RequiredArgsConstructor
public class ProjectImageController {

    private final ProjectImageService projectImageService;

    @GetMapping
    public ResponseEntity<List<ProjectImage>> getAll() {
        return ResponseEntity.ok(projectImageService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectImage> getById(@PathVariable UUID id) {
        ProjectImage image = projectImageService.getById(id);
        if (image != null) {
            return ResponseEntity.ok(image);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<ProjectImage> create(@RequestBody ProjectImage projectImage) {
        return ResponseEntity.ok(projectImageService.create(projectImage));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        projectImageService.delete(id);
        return ResponseEntity.ok().build();
    }
}

