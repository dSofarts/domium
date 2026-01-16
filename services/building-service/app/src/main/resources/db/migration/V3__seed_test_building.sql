-- Тестовые пользователи из infra/keycloak/domium-realm.json:
-- test-client  -> c1d2e3f4-a5b6-7890-cdef-123456789012
-- test-manager -> d2e3f4a5-b6c7-8901-def2-234567890123

-- Одна тестовая стройка + projection для отображения в списках.
-- Идемпотентно: при повторном прогоне ничего не сломается.

INSERT INTO buildings (
    id,
    project_id,
    client_id,
    manager_id,
    workflow_id,
    current_stage_id,
    current_stage_name,
    current_stage_index,
    progress,
    status,
    created_at
)
VALUES (
    '12d05685-c6d9-41ab-a660-17460ffa1824',
    '3fa85f64-5717-4562-b3fc-2c963f66afa6',
    'c1d2e3f4-a5b6-7890-cdef-123456789012',
    'd2e3f4a5-b6c7-8901-def2-234567890123',
    '11111111-1111-1111-1111-111111111111',
    '22222222-2222-2222-2222-222222222221',
    'Подготовительный этап',
    0,
    0,
    'ACTIVE',
    NOW()
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO building_projections (
    id,
    project_id,
    project_name,
    client_id,
    manager_id,
    manager_name,
    stage,
    progress,
    video_url,
    updated_at,
    metadata
)
VALUES (
    '12d05685-c6d9-41ab-a660-17460ffa1824',
    '3fa85f64-5717-4562-b3fc-2c963f66afa6',
    'Demo Project',
    'c1d2e3f4-a5b6-7890-cdef-123456789012',
    'd2e3f4a5-b6c7-8901-def2-234567890123',
    'Test Manager',
    'Подготовительный этап',
    0,
    'https://sample-videos.com/zip/10/mp4/SampleVideo_1280x720_1mb.mp4',
    NOW(),
    '{}'::jsonb
)
ON CONFLICT (id) DO NOTHING;

