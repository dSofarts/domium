package ru.domium.building.it;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import ru.domium.building.api.dto.workflow.UpsertStagesRequest;
import ru.domium.building.api.dto.workflow.WorkflowDto;
import ru.domium.building.service.WorkflowService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class WorkflowServiceIT extends AbstractPostgresIT {

    private static final UUID DEFAULT_WORKFLOW_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID RANDOM_MANAGER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Autowired
    WorkflowService workflowService;

    @Test
    void get_defaultWorkflow_isReadableByAnyManager() {
        WorkflowDto dto = workflowService.get(DEFAULT_WORKFLOW_ID, RANDOM_MANAGER_ID);
        assertThat(dto.getId()).isEqualTo(DEFAULT_WORKFLOW_ID);
        assertThat(dto.getStages()).isNotEmpty();
    }

    @Test
    void getActive_fallsBackToDefault_whenManagerHasNoWorkflow() {
        WorkflowDto dto = workflowService.getActive(RANDOM_MANAGER_ID);
        assertThat(dto.getId()).isEqualTo(DEFAULT_WORKFLOW_ID);
        assertThat(dto.getStages()).isNotEmpty();
    }

    @Test
    void cannotModifyDefaultWorkflow_upsertStagesIsForbidden() {
        UpsertStagesRequest req = new UpsertStagesRequest();
        req.setStages(java.util.List.of());

        assertThatThrownBy(() -> workflowService.upsertStages(DEFAULT_WORKFLOW_ID, RANDOM_MANAGER_ID, req))
                .isInstanceOf(AccessDeniedException.class);
    }
}
