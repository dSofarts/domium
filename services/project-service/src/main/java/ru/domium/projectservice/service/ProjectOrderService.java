package ru.domium.projectservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.domium.projectservice.dto.response.ProjectOrderResponse;
import ru.domium.projectservice.entity.Project;
import ru.domium.projectservice.entity.ProjectOrder;
import ru.domium.projectservice.entity.ProjectOrderStatus;
import ru.domium.projectservice.exception.NotAccessException;
import ru.domium.projectservice.exception.NotFoundException;
import ru.domium.projectservice.repository.ProjectOrderRepository;
import ru.domium.projectservice.repository.ProjectRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectOrderService {

    private final ProjectOrderRepository projectOrderRepository;
    private final ProjectRepository projectRepository;
    private final ModelMapper modelMapper;

    public ProjectOrderResponse createOrder(UUID projectId, UUID clientId) {
        log.info("Создание заказа по проекту: projectId={}, clientId={}", projectId, clientId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> {
                    log.warn("Проект не найден при создании заказа: projectId={}, clientId={}", projectId, clientId);
                    return NotFoundException.projectNotFound(projectId);
                });

        ProjectOrder projectOrder = ProjectOrder.builder()
                .project(project)
                .clientUserId(clientId)
                .status(ProjectOrderStatus.PENDING)
                .build();

        ProjectOrder savedOrder = projectOrderRepository.save(projectOrder);
        log.info("Заказ создан: orderId={}, projectId={}, clientId={}, status={}",
                savedOrder.getId(), projectId, clientId, savedOrder.getStatus());

        return mapToResponse(savedOrder);
    }

    public List<ProjectOrderResponse> getAllByManagerId(UUID managerId) {
        List<Project> projects = projectRepository.findAllByManagerUserId(managerId);
        List<ProjectOrderResponse> orders = projectOrderRepository.findAllByProjectIn(projects).stream()
                .map(this::mapToResponse)
                .toList();
        log.debug("Получены все заказы: count={}", orders.size());
        return orders;
    }

    public ProjectOrderResponse getByIdAndManagerId(UUID orderId, UUID managerId) {
        ProjectOrder order = projectOrderRepository.findById(orderId).orElseThrow(() -> {
            log.warn("Заказ не найден для менеджера: orderId={}, managerId={}", orderId, managerId);
            return NotFoundException.projectOrderNotFound(orderId);
        });

        UUID projectId = order.getProject().getId();
        Project project = projectRepository.findById(projectId).orElseThrow(() -> {
            log.warn("Проект не найден для заказа: orderId={}, projectId={}", orderId, projectId);
            return NotFoundException.projectNotFound(projectId);
        });

        if (!project.getManagerUserId().equals(managerId)) {
            log.warn("Менеджер managerId={} не имеет доступа к заказу: orderId={}", orderId, managerId);
            throw new NotAccessException(managerId, projectId);
        }

        log.debug("Заказ найден: orderId={}, projectId={}, clientId={}, status={}",
                order.getId(),
                order.getProject() != null ? order.getProject().getId() : null,
                order.getClientUserId(),
                order.getStatus());
        return mapToResponse(order);
    }

    public List<ProjectOrderResponse> getAllByClientId(UUID clientId) {
        List<ProjectOrder> orders = projectOrderRepository.findAllByClientUserId(clientId);
        log.debug("Получены заказы клиента: clientId={}, count={}", clientId, orders.size());

        return orders.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public ResponseEntity<ProjectOrderResponse> getByIdAndClientId(UUID orderId, UUID clientId) {
        log.debug("Получение заказа по id и клиенту: orderId={}, clientId={}", orderId, clientId);

        ProjectOrder order = projectOrderRepository.findByIdAndClientUserId(orderId, clientId)
                .orElseThrow(() -> {
                    log.warn("Заказ не найден для клиента: orderId={}, clientId={}", orderId, clientId);
                    // оставляю вашу текущую фабрику исключения, чтобы не ломать контракт
                    return NotFoundException.projectNotFound(orderId);
                });

        log.debug("Заказ найден для клиента: orderId={}, projectId={}, clientId={}, status={}",
                order.getId(),
                order.getProject() != null ? order.getProject().getId() : null,
                order.getClientUserId(),
                order.getStatus());

        return ResponseEntity.ok(mapToResponse(order));
    }

    public List<ProjectOrderResponse> getAllByProjectIdAndManagerId(UUID projectId, UUID managerId) {
        log.debug("Получение заказов по проекту для менеджера: projectId={}, managerId={}", projectId, managerId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> {
                    log.warn("Проект не найден при получении заказов: projectId={}, managerId={}", projectId, managerId);
                    return NotFoundException.projectNotFound(projectId);
                });

        if (!project.getManagerUserId().equals(managerId)) {
            log.warn("Отказ в доступе при получении заказов по проекту: projectId={}, managerId={}, projectManagerId={}",
                    projectId, managerId, project.getManagerUserId());
            throw new NotAccessException(managerId, projectId);
        }

        List<ProjectOrderResponse> orders = projectOrderRepository
                .findAllByProject_IdAndProject_ManagerUserId(projectId, managerId)
                .stream()
                .map(this::mapToResponse)
                .toList();

        log.debug("Получены заказы по проекту для менеджера: projectId={}, managerId={}, count={}", projectId, managerId, orders.size());
        return orders;
    }

    private ProjectOrderResponse mapToResponse(ProjectOrder order) {
        ProjectOrderResponse response = modelMapper.map(order, ProjectOrderResponse.class);
        response.setProjectId(order.getProject().getId());
        return response;
    }
}
