package ru.domium.projectservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
public class CreateFloorRequest {

    @Min(value = -5, message = "Номер этажа не может быть меньше -5")
    private int floorNumber;

    @Valid
    @NotEmpty(message = "Список комнат не может быть пустым")
    private List<CreateRoomRequest> rooms;
}
