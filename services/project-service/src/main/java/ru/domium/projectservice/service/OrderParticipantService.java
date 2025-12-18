package ru.domium.projectservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.domium.projectservice.dto.response.OrderParticipantResponse;
import ru.domium.projectservice.entity.OrderParticipant;
import ru.domium.projectservice.repository.OrderParticipantRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderParticipantService {

    private final OrderParticipantRepository orderParticipantRepository;

    public List<OrderParticipant> getAll() {
        return orderParticipantRepository.findAll();
    }

    public OrderParticipant getById(UUID id) {
        return orderParticipantRepository.findById(id).orElse(null);
    }

    public OrderParticipant create(OrderParticipant orderParticipant) {
        return orderParticipantRepository.save(orderParticipant);
    }

    public void delete(UUID id) {
        orderParticipantRepository.deleteById(id);
    }

    public OrderParticipantResponse assignParticipant(UUID projectId, UUID orderId, UUID userId) {
        return null;
    }

    public void unassignParticipant(UUID projectId, UUID orderId, UUID userId) {
    }
}

