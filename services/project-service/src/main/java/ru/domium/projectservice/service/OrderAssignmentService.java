package ru.domium.projectservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.domium.projectservice.entity.OrderAssignment;
import ru.domium.projectservice.repository.OrderAssignmentRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderAssignmentService {

    private final OrderAssignmentRepository orderAssignmentRepository;

    public List<OrderAssignment> getAll() {
        return orderAssignmentRepository.findAll();
    }

    public OrderAssignment getById(UUID id) {
        return orderAssignmentRepository.findById(id).orElse(null);
    }

    public OrderAssignment create(OrderAssignment orderAssignment) {
        return orderAssignmentRepository.save(orderAssignment);
    }

    public void delete(UUID id) {
        orderAssignmentRepository.deleteById(id);
    }
}

