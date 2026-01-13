package ru.domium.projectservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectOrderResponse {
    private UUID id;
    private UUID projectId;
    private UUID clientUserId;
    private String status;
}
