package ru.domium.projectservice.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.domium.projectservice.dto.request.CreateProjectRequest;
import ru.domium.projectservice.dto.request.CreateRoomRequest;
import ru.domium.projectservice.dto.request.UpdateRoomRequest;
import ru.domium.projectservice.entity.Project;
import ru.domium.projectservice.entity.Room;

@Configuration
public class MapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();
        mapper.typeMap(CreateProjectRequest.class, Project.class)
                .addMappings(mapping -> {
                    mapping.skip(Project::setId);
                    mapping.map(CreateProjectRequest::getWorkflowId, Project::setWorkflowId);
                });
        mapper.typeMap(CreateRoomRequest.class, Room.class)
                .addMapping(CreateRoomRequest::getType, Room::setRoomType);
        mapper.typeMap(UpdateRoomRequest.class, Room.class)
                .addMapping(UpdateRoomRequest::getType, Room::setRoomType);
        return mapper;
    }
}
