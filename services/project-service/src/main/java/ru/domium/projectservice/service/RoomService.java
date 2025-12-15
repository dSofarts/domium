package ru.domium.projectservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.domium.projectservice.entity.Room;
import ru.domium.projectservice.repository.RoomRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;

    public List<Room> getAll() {
        return roomRepository.findAll();
    }

    public Room getById(UUID id) {
        return roomRepository.findById(id).orElse(null);
    }

    public Room create(Room room) {
        return roomRepository.save(room);
    }

    public void delete(UUID id) {
        roomRepository.deleteById(id);
    }
}

