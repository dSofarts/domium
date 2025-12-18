package ru.domium.projectservice.service;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import ru.domium.projectservice.dto.response.ProjectOrderResponse;
import ru.domium.projectservice.entity.Project;
import ru.domium.projectservice.entity.ProjectOrder;
import ru.domium.projectservice.entity.ProjectOrderStatus;
import ru.domium.projectservice.exception.NotFoundException;
import ru.domium.projectservice.repository.ProjectOrderRepository;
import ru.domium.projectservice.repository.ProjectRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectOrderService {

    private final ProjectOrderRepository projectOrderRepository;
    private final ProjectRepository projectRepository;
    private final ModelMapper modelMapper;

    public ProjectOrderResponse createOrder(UUID projectId) {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> NotFoundException.projectNotFound(projectId));

        ProjectOrder projectOrder = ProjectOrder.builder()
                .project(project)
                .clientUserId(UUID.randomUUID()) //TODO: Get user from Authentication context
                .status(ProjectOrderStatus.PENDING)
                .build();

//        TODO: Call building-service to process the order

        ProjectOrder savedOrder = projectOrderRepository.save(projectOrder);
        return mapToResponse(savedOrder);
    }

    private ProjectOrderResponse mapToResponse(ProjectOrder order) {
        ProjectOrderResponse response = modelMapper.map(order, ProjectOrderResponse.class);
        response.setProjectId(order.getProject().getId());
        return response;
    }

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

