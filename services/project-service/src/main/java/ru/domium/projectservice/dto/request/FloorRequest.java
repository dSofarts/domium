package ru.domium.projectservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class FloorRequest {

    @Min(-5)
    private int floorNumber;
    @Valid
    @NotEmpty
    private List<RoomRequest> rooms;
}
