package ru.domium.projectservice.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class DeleteProjectImagesRequest {
    @NotEmpty(message = "Список ID изображений для удаления не может быть пустым")
    List<UUID> imageIds;
}
