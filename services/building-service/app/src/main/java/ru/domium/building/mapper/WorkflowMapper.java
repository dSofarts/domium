package ru.domium.building.mapper;

import org.mapstruct.Mapper;
import ru.domium.building.api.dto.workflow.WorkflowDto;
import ru.domium.building.api.dto.workflow.StageDto;
import ru.domium.building.api.dto.workflow.SubStageDto;
import ru.domium.building.api.dto.workitem.WorkItemDto;
import ru.domium.building.model.Workflow;
import ru.domium.building.model.Stage;
import ru.domium.building.model.Task;

import java.util.List;

@Mapper(componentModel = "spring")
public interface WorkflowMapper {
    WorkflowDto toDto(Workflow workflow);
    StageDto toDto(Stage stage);
    List<StageDto> toStageDtos(List<Stage> stages);

    SubStageDto toSubStageDto(Task task);
    List<SubStageDto> toSubStageDtos(List<Task> tasks);

    WorkItemDto toWorkItemDto(Task task);
    List<WorkItemDto> toWorkItemDtos(List<Task> tasks);
}
