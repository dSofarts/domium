package ru.domium.building.api.dto.video;

import lombok.Data;

import java.util.UUID;

@Data
public class CameraDto {
    private UUID id;
    private UUID buildingId;
    private String name;
    private boolean enabled;
    private boolean transcode;

    /**
     * Относительный URL плейлиста (m3u8), например: /hls/{buildingId}/{cameraId}/index.m3u8
     */
    private String hlsUrl;

    private boolean running;
}


