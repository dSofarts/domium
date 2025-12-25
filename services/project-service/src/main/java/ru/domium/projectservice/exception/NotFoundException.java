package ru.domium.projectservice.exception;

import java.util.UUID;

public class NotFoundException extends ProjectsException{

    private NotFoundException(String message) {
        super(message);
    }

    public static RuntimeException projectNotFound(UUID projectId) {
        throw new NotFoundException("Project with id " + projectId + " not found");
    }
}
