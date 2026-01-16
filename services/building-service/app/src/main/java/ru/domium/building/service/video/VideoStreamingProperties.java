package ru.domium.building.service.video;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Data
@Configuration
@ConfigurationProperties(prefix = "domium.video")
public class VideoStreamingProperties {
    /**
     * Корневая директория для HLS (должна быть общей с nginx через volume).
     */
    private Path hlsRoot = Path.of("/var/domium/video/hls");

    /**
     * Длина HLS сегмента (сек). Для задержки ~<=5с обычно 1.
     */
    private int hlsTimeSeconds = 1;

    /**
     * Размер плейлиста (кол-во сегментов). Для задержки ~<=5с обычно 4-5.
     */
    private int hlsListSize = 4;

    /**
     * Авто-останов по неактивности (нет запросов HLS).
     */
    private boolean autoStopEnabled = true;

    /**
     * Останавливать поток, если не было запросов дольше N секунд.
     */
    private int idleTimeoutSeconds = 120;

    /**
     * Как часто проверять неактивные потоки (мс).
     */
    private long idleCheckPeriodMs = 30_000;
}


