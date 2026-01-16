package ru.domium.building.service.video;

import java.nio.file.Path;
import java.util.UUID;

public record VideoPaths(Path hlsDir) {
    public Path hlsPlaylist() {
        return hlsDir.resolve("index.m3u8");
    }

    public static String hlsUrl(UUID buildingId, UUID cameraId) {
        return "/hls/" + buildingId + "/" + cameraId + "/index.m3u8";
    }
}


