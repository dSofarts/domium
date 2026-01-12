package ru.domium.projectservice.dto.response;

import lombok.*;

import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProjectImageResponse {
    private UUID id;
    private String imageUrl;
    private Integer position;
}
