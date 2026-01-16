package ru.domium.building.service.video;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.domium.building.model.BuildingCamera;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class FfmpegStreamManager {
    private static final int MAX_FFMPEG_LOG_LINES = 200;

    private final VideoStreamingProperties props;
    private final Map<UUID, Process> processes = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastAccessMs = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<String>> lastFfmpegOutput = new ConcurrentHashMap<>();

    public boolean isRunning(UUID cameraId) {
        Process p = processes.get(cameraId);
        return p != null && p.isAlive();
    }

    /**
     * Обновляет активность потока. Вызывать при каждом успешном запросе HLS (через auth_request).
     */
    public void touch(UUID cameraId) {
        if (cameraId == null) return;
        lastAccessMs.put(cameraId, System.currentTimeMillis());
    }

    public VideoPaths paths(UUID buildingId, UUID cameraId) {
        return new VideoPaths(
                props.getHlsRoot().resolve(buildingId.toString()).resolve(cameraId.toString())
        );
    }

    public synchronized void start(UUID buildingId, BuildingCamera camera) {
        if (!camera.isEnabled()) {
            throw new IllegalStateException("Camera is disabled: " + camera.getId());
        }
        UUID cameraId = camera.getId();
        if (cameraId == null) throw new IllegalArgumentException("camera.id is required");
        if (isRunning(cameraId)) return;
        touch(cameraId);

        VideoPaths paths = paths(buildingId, cameraId);
        try {
            Files.createDirectories(paths.hlsDir());
        } catch (Exception e) {
            throw new IllegalStateException("Cannot create video directories", e);
        }

        String hlsDir = paths.hlsDir().toString();

        String hlsPlaylist = hlsDir + "/index.m3u8";

        List<String> cmd = new ArrayList<>();
        cmd.add("ffmpeg");
        cmd.add("-hide_banner");
        cmd.add("-loglevel");
        cmd.add("warning");
        cmd.add("-rtsp_transport");
        cmd.add("tcp");
        cmd.add("-fflags");
        cmd.add("nobuffer");
        cmd.add("-flags");
        cmd.add("low_delay");
        cmd.add("-i");
        cmd.add(camera.getRtspUrl());
        cmd.add("-map");
        cmd.add("0:v:0");

        if (camera.isTranscode()) {
            // Предсказуемый H264 для браузера + частые ключевые кадры под сегментацию.
            cmd.add("-c:v");
            cmd.add("libx264");
            cmd.add("-preset");
            cmd.add("veryfast");
            cmd.add("-tune");
            cmd.add("zerolatency");
            cmd.add("-pix_fmt");
            cmd.add("yuv420p");
            cmd.add("-g");
            cmd.add(String.valueOf(Math.max(10, props.getHlsTimeSeconds() * 25)));
            cmd.add("-keyint_min");
            cmd.add(String.valueOf(Math.max(10, props.getHlsTimeSeconds() * 25)));
            cmd.add("-sc_threshold");
            cmd.add("0");
        } else {
            cmd.add("-c:v");
            cmd.add("copy");
        }

        // Аудио для стройки обычно не нужно, и часто ломает совместимость.
        cmd.add("-an");

        cmd.add("-f");
        cmd.add("hls");
        cmd.add("-hls_time");
        cmd.add(String.valueOf(props.getHlsTimeSeconds()));
        cmd.add("-hls_list_size");
        cmd.add(String.valueOf(props.getHlsListSize()));
        cmd.add("-hls_flags");
        cmd.add("delete_segments+append_list+independent_segments");
        cmd.add("-hls_segment_filename");
        cmd.add(hlsDir + "/seg_%08d.ts");
        cmd.add(hlsPlaylist);

        try {
            Process p = new ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .start();
            processes.put(cameraId, p);
            lastFfmpegOutput.put(cameraId, new ConcurrentLinkedDeque<>());
            touch(cameraId);
            log.info("FFmpeg started for camera {} (building {})", cameraId, buildingId);

            // Не блокируем: читаем вывод, чтобы не забить буфер процесса.
            Thread t = new Thread(() -> drainLogs(cameraId, p), "ffmpeg-" + cameraId);
            t.setDaemon(true);
            t.start();

            p.onExit().thenRun(() -> {
                processes.remove(cameraId, p);
                lastAccessMs.remove(cameraId);
                int code = p.exitValue();
                Deque<String> out = lastFfmpegOutput.remove(cameraId);
                if (code == 0) {
                    log.info("FFmpeg exited for camera {} (code=0)", cameraId);
                    return;
                }

                String details = formatLastFfmpegLines(out);
                if (details.isBlank()) {
                    log.warn("FFmpeg exited for camera {} (code={})", cameraId, code);
                } else {
                    log.warn("FFmpeg exited for camera {} (code={}). Last ffmpeg output:\n{}", cameraId, code, details);
                }
            });
        } catch (Exception e) {
            throw new IllegalStateException("Cannot start ffmpeg for camera " + cameraId, e);
        }
    }

    public synchronized void stop(UUID cameraId) {
        Process p = processes.remove(cameraId);
        lastAccessMs.remove(cameraId);
        if (p == null) return;
        if (!p.isAlive()) return;

        p.destroy();
        try {
            p.waitFor();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        if (p.isAlive()) {
            p.destroyForcibly();
        }
        log.info("FFmpeg stopped for camera {}", cameraId);
    }

    @Scheduled(fixedDelayString = "${domium.video.idle-check-period-ms:30000}")
    public void stopIdleStreams() {
        if (!props.isAutoStopEnabled()) return;
        long now = System.currentTimeMillis();
        long idleMs = Math.max(1, props.getIdleTimeoutSeconds()) * 1000L;
        for (UUID cameraId : List.copyOf(processes.keySet())) {
            Process p = processes.get(cameraId);
            if (p == null || !p.isAlive()) continue;
            Long last = lastAccessMs.get(cameraId);
            if (last == null) continue; // нет данных — не трогаем
            if (now - last > idleMs) {
                log.info("Auto-stopping idle camera {} (idle={}ms)", cameraId, (now - last));
                try {
                    stop(cameraId);
                } catch (Exception e) {
                    log.warn("Failed to auto-stop camera {}", cameraId, e);
                }
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        for (UUID cameraId : List.copyOf(processes.keySet())) {
            try {
                stop(cameraId);
            } catch (Exception ignored) {
                // best-effort
            }
        }
    }

    private void drainLogs(UUID cameraId, Process p) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            Deque<String> buf = lastFfmpegOutput.computeIfAbsent(cameraId, id -> new ConcurrentLinkedDeque<>());
            String line;
            while ((line = br.readLine()) != null) {
                // warning-уровень обычно достаточно шумный, оставляем debug
                log.debug("[ffmpeg:{}] {}", cameraId, line);

                buf.addLast(line);
                while (buf.size() > MAX_FFMPEG_LOG_LINES) {
                    buf.pollFirst();
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static String formatLastFfmpegLines(Deque<String> out) {
        if (out == null || out.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String line : out) {
            if (line == null) continue;
            sb.append(line).append('\n');
        }
        return sb.toString().trim();
    }
}


