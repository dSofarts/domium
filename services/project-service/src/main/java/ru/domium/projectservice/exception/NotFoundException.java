package ru.domium.projectservice.exception;

import java.util.UUID;

public class NotFoundException extends ProjectsException {

    private NotFoundException(String message) {
        super(ProjectErrorType.NOT_FOUND, message);
    }

    public static RuntimeException projectNotFound(UUID projectId) {
        throw new NotFoundException("Project with id " + projectId + " not found");
    }

    public static RuntimeException projectImageNotFound(UUID imageId, UUID projectId) {
        throw new NotFoundException("Project image with id " + imageId + " for project with id " + projectId + " not found");
    }

    public static RuntimeException projectOrderNotFound(UUID orderId) {
        throw new NotFoundException("Project order with id " + orderId + " not found");
    }
}
