package ru.domium.projectservice.event;

import java.util.List;
import java.util.UUID;

public record ProjectDeletedEvent(UUID projectId, List<String> imageKeys) {}
