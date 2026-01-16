-- Одна тестовая камера для сидовой стройки.
-- RTSP берём из локального эмулятора (см. docker-compose: rtsp-server/rtsp-publisher):
-- rtsp://rtsp-server:8554/test

INSERT INTO building_cameras (
    id,
    building_id,
    name,
    rtsp_url,
    enabled,
    transcode,
    created_at,
    updated_at
)
VALUES (
    'b80ebc8b-2f35-439c-8941-fdc924ceda8b',
    '12d05685-c6d9-41ab-a660-17460ffa1824',
    'test',
    'rtsp://rtsp-server:8554/test',
    true,
    true,
    NOW(),
    NOW()
)
ON CONFLICT (id) DO NOTHING;

