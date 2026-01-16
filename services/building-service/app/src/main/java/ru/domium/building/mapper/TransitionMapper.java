package ru.domium.building.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.domium.building.api.dto.transition.StageTransitionDeniedDto;
import ru.domium.building.api.dto.transition.TransitionViolationDto;
import ru.domium.building.service.stage.requirement.TransitionViolation;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface TransitionMapper {
    TransitionViolationDto toDto(TransitionViolation violation);
    List<TransitionViolationDto> toDtos(List<TransitionViolation> violations);

    @Mapping(target = "buildingId", source = "buildingId")
    @Mapping(target = "violations", source = "violations")
    StageTransitionDeniedDto toDenied(UUID buildingId, List<TransitionViolation> violations);
}
