package ru.domium.projectservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.domium.projectservice.entity.Floor;
import ru.domium.projectservice.service.FloorService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/floors")
@RequiredArgsConstructor
public class FloorController {

    private final FloorService floorService;

    @GetMapping
    public ResponseEntity<List<Floor>> getAll() {
        return ResponseEntity.ok(floorService.getAllFloors());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Floor> getById(@PathVariable UUID id) {
        Floor floor = floorService.getFloorById(id);
        if (floor != null) {
            return ResponseEntity.ok(floor);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<Floor> create(@RequestBody Floor floor) {
        return ResponseEntity.ok(floorService.createFloor(floor));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        floorService.deleteFloor(id);
        return ResponseEntity.ok().build();
    }
}

