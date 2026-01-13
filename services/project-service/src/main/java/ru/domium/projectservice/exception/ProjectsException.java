package ru.domium.projectservice.exception;

public class ProjectsException extends RuntimeException {

    private final ProjectErrorType errorType;

    public ProjectsException(ProjectErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public ProjectsException(ProjectErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    public ProjectErrorType getErrorType() {
        return errorType;
    }
}
