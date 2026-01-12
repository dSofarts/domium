package ru.domium.projectservice.exception;

import java.util.UUID;

public class NotAccessException extends ProjectsException {
    public NotAccessException(UUID userId, UUID projectId) {
        super("User " + userId + " does not have access to project " + projectId);
    }
}
