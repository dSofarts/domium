package ru.domium.projectservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Deprecated
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderParticipantResponse {
    private UUID id;
    private UUID orderId;
    private UUID userId;
    private String participantRole;
}
