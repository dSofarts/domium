package ru.domium.projectservice.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ReorderImagesRequest {
    private List<UUID> orderedImageIds;
}
