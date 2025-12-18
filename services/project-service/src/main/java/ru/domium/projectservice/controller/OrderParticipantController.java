package ru.domium.projectservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.domium.projectservice.dto.response.OrderParticipantResponse;
import ru.domium.projectservice.entity.OrderParticipant;
import ru.domium.projectservice.service.OrderParticipantService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/projects/{projectId}/orders/{orderId}/participants")
@RequiredArgsConstructor
public class OrderParticipantController {

    private final OrderParticipantService orderParticipantService;

//    @PreAuthorize("hasRole('OWNER')")
    @GetMapping
    public ResponseEntity<List<OrderParticipant>> getAll(@PathVariable UUID projectId,
                                                         @PathVariable UUID orderId) {
//        TODO: Correct implementation
        return ResponseEntity.ok(orderParticipantService.getAll());
    }

    //    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/participants/{userId}")
    public ResponseEntity<OrderParticipantResponse> assignParticipant(@PathVariable UUID projectId,
                                                                      @PathVariable UUID orderId,
                                                                      @PathVariable UUID userId) {
//        TODO: Correct implementation
        OrderParticipantResponse participant = orderParticipantService.assignParticipant(projectId, orderId, userId);
        return ResponseEntity.ok(participant);
    }

    //    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/participants/{userId}/unassign")
    public ResponseEntity<Void> unassignParticipant(@PathVariable UUID projectId,
                                                    @PathVariable UUID orderId,
                                                    @PathVariable UUID userId) {
//    TODO: Correct implementation
        orderParticipantService.unassignParticipant(projectId, orderId, userId);
        return ResponseEntity.ok().build();
    }


    //    @PreAuthorize("hasRole('OWNER')")
    @GetMapping("/{participantId}")
    public ResponseEntity<OrderParticipant> getById(@PathVariable String projectId,
                                                    @PathVariable String orderId,
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

