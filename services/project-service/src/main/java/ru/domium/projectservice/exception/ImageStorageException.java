package ru.domium.projectservice.exception;

public class ImageStorageException extends ProjectsException {
    public ImageStorageException(String message) {
        super(ProjectErrorType.IMAGE_STORAGE, message);
    }

    public ImageStorageException(String message, Throwable cause) {
        super(ProjectErrorType.IMAGE_STORAGE, message, cause);
    }
}
