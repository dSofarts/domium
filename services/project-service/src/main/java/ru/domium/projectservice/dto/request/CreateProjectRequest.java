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
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProjectRequest {
    @NotNull
    private UUID managerUserId;

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotNull
    private ProjectType type;

    @NotBlank
    @Size(max = 100)
    private String category;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal price;

    @NotBlank
    @Size(max = 100)
    private String material;

    @NotBlank
    @Size(max = 255)
    private String location;

    @Size(max = 2000)
    private String description;

    @Valid
    @NotEmpty
    private List<FloorRequest> floors = new ArrayList<>();
}
