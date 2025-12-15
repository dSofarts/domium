package ru.domium.projectservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.domium.projectservice.entity.Floor;
import ru.domium.projectservice.repository.FloorRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FloorService {

    private final FloorRepository floorRepository;

    public List<Floor> getAllFloors() {
        return floorRepository.findAll();
    }

    public Floor getFloorById(UUID id) {
        return floorRepository.findById(id).orElse(null);
    }

    public Floor createFloor(Floor floor) {
        return floorRepository.save(floor);
    }

    public void deleteFloor(UUID id) {
        floorRepository.deleteById(id);
    }
}

