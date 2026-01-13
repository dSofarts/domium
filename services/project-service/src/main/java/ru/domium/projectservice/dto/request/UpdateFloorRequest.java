package ru.domium.projectservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class UpdateFloorRequest {

    private UUID id;

    @Min(value = -5, message = "Номер этажа не может быть меньше -5")
    private int floorNumber;

    @Valid
    private List<UpdateRoomRequest> rooms;
}
