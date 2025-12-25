package ru.domium.projectservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.domium.projectservice.dto.response.OrderParticipantResponse;
import ru.domium.projectservice.entity.OrderParticipant;
import ru.domium.projectservice.service.OrderParticipantService;
import ru.domium.security.annotation.PublicEndpoint;

import java.util.List;
import java.util.UUID;

/**
Функционал с участниками проекта на стороне Building Service
 */
@Deprecated
@RestController
@RequestMapping(value = "/projects/{projectId}/orders/{orderId}/participants")
@RequiredArgsConstructor
public class OrderParticipantController {

    private final OrderParticipantService orderParticipantService;

    @GetMapping
    public ResponseEntity<List<OrderParticipant>> getAll(@PathVariable UUID projectId,
                                                         @PathVariable UUID orderId) {
//        TODO: Correct implementation
        return ResponseEntity.ok(orderParticipantService.getAll());
    }

    @PostMapping("/participants/{userId}")
    public ResponseEntity<OrderParticipantResponse> assignParticipant(@PathVariable UUID projectId,
                                                                      @PathVariable UUID orderId,
                                                                      @PathVariable UUID userId) {
//        TODO: Correct implementation
        OrderParticipantResponse participant = orderParticipantService.assignParticipant(projectId, orderId, userId);
        return ResponseEntity.ok(participant);
    }

    @PostMapping("/participants/{userId}/unassign")
    public ResponseEntity<Void> unassignParticipant(@PathVariable UUID projectId,
                                                    @PathVariable UUID orderId,
                                                    @PathVariable UUID userId) {
//    TODO: Correct implementation
        orderParticipantService.unassignParticipant(projectId, orderId, userId);
        return ResponseEntity.ok().build();
    }


    @GetMapping("/{participantId}")
    public ResponseEntity<OrderParticipant> getById(@PathVariable UUID projectId,
                                                    @PathVariable UUID orderId,
                                                    @PathVariable UUID participantId) {
//    TODO: Correct implementation
        OrderParticipant orderParticipant = orderParticipantService.getById(participantId);
        if (orderParticipant != null) {
            return ResponseEntity.ok(orderParticipant);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

