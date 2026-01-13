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
public class UpdateProjectRequest {
    @Size(max = 255, message = "Название проекта не должно превышать 255 символов")
    private String name;

    private ProjectType type;

    @Size(max = 100, message = "Категория проекта не должна превышать 100 символов")
    private String category;

    @DecimalMin(value = "0.0", inclusive = false, message = "Стоимость проекта должна быть больше 0")
    private BigDecimal price;

    @Size(max = 100, message = "Материал не должен превышать 100 символов")
    private String material;

    @Size(max = 255, message = "Локация не должна превышать 255 символов")
    private String location;

    @Size(max = 2000, message = "Описание не должно превышать 2000 символов")
    private String description;

    @Valid
    private List<UpdateFloorRequest> floors = new ArrayList<>();
}
