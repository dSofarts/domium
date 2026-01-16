CREATE TABLE IF NOT EXISTS building_cameras (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    building_id UUID NOT NULL,
    name TEXT NOT NULL,
    rtsp_url TEXT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    transcode BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_building_cameras_building_id ON building_cameras(building_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_building_cameras_building_name
    ON building_cameras(building_id, name);


