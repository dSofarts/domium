package ru.domium.projectservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.domium.projectservice.dto.response.ProjectImageResponse;
import ru.domium.projectservice.service.ProjectImageService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/projects/{projectId}/images")
@RequiredArgsConstructor
public class ProjectImageController {

    private final ProjectImageService imageService;

    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<ProjectImageResponse> addImages(@PathVariable UUID projectId,
                                                @RequestPart("images") List<MultipartFile> images) {
        return imageService.addImagesToProject(projectId, images);
    }
}

