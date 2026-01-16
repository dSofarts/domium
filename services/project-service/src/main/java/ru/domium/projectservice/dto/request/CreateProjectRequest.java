package ru.domium.projectservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.domium.projectservice.entity.ProjectType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProjectRequest {
    @NotBlank(message = "Название проекта обязательно")
    @Size(max = 255, message = "Название проекта не должно превышать 255 символов")
    private String name;

    @NotNull(message = "Тип проекта обязателен (SERIAL, INDIVIDUAL, BATHHOUSE)")
    private ProjectType type;

    @NotBlank(message = "Категория проекта обязательна")
    @Size(max = 100, message = "Категория проекта не должна превышать 100 символов")
    private String category;

    @NotNull(message = "Стоимость проекта обязательна")
    @DecimalMin(value = "0.0", inclusive = false, message = "Стоимость проекта должна быть больше 0")
    private BigDecimal price;

    @NotBlank(message = "Материал обязателен")
    @Size(max = 100, message = "Материал не должен превышать 100 символов")
    private String material;

    @NotBlank(message = "Локация обязательна")
    @Size(max = 255, message = "Локация не должна превышать 255 символов")
    private String location;

    @Size(max = 2000, message = "Описание не должно превышать 2000 символов")
    private String description;

    private java.util.UUID workflowId;

    @Valid
    @NotEmpty(message = "Список этажей не может быть пустым")
    private List<CreateFloorRequest> floors = new ArrayList<>();
}
