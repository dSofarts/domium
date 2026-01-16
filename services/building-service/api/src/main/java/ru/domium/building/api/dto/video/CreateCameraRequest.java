package ru.domium.building.api.dto.video;

import lombok.Data;

@Data
public class CreateCameraRequest {
    private String name;
    private String rtspUrl;
    /**
     * Если камера отдаёт H265/нестабильный GOP — включите перекодирование в H264.
     */
    private Boolean transcode;
}


