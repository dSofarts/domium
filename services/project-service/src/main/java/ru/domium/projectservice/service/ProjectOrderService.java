package ru.domium.projectservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.domium.projectservice.entity.ProjectOrder;
import ru.domium.projectservice.repository.ProjectOrderRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectOrderService {

    private final ProjectOrderRepository projectOrderRepository;

    public List<ProjectOrder> getAll() {
        return projectOrderRepository.findAll();
    }

    public ProjectOrder getById(UUID id) {
        return projectOrderRepository.findById(id).orElse(null);
    }

    public ProjectOrder create(ProjectOrder order) {
        return projectOrderRepository.save(order);
    }

    public void delete(UUID id) {
        projectOrderRepository.deleteById(id);
    }
}

