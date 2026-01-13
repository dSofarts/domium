package ru.domium.projectservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Тип (категория) ошибки для кастомных исключений project-service.
 */
@Getter
public enum ProjectErrorType {

    ACCESS_DENIED(HttpStatus.FORBIDDEN, "Access denied", "Доступ запрещен"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found", "Ресурс не найден"),
    IMAGE_STORAGE(HttpStatus.INTERNAL_SERVER_ERROR, "Image storage error", "Ошибка хранения изображения"),
    VALIDATION(HttpStatus.BAD_REQUEST, "Validation failed", "Ошибка валидации"),
    INTERNAL(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", "Внутренняя ошибка");

    private final HttpStatus httpStatus;
    private final String title;
    private final String defaultDetail;

    ProjectErrorType(HttpStatus httpStatus, String title, String defaultDetail) {
        this.httpStatus = httpStatus;
        this.title = title;
        this.defaultDetail = defaultDetail;
    }

}

