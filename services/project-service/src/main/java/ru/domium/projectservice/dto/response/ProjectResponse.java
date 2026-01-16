package ru.domium.projectservice.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {
    private UUID id;
    private UUID managerUserId;
    private String name;
    private String type;
    private String category;
    private BigDecimal price;
    private String material;
    private String location;
    private String description;
    private java.util.UUID workflowId;
    @Builder.Default
    private List<FloorResponse> floors = new ArrayList<>();
    @Builder.Default
    private List<ProjectImageResponse> imageUrls = new ArrayList<>();
}
