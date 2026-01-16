package ru.domium.building.service.stage.requirement;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import ru.domium.building.client.DocumentServiceClient;
import ru.domium.building.model.Building;
import ru.domium.building.model.StageTransition;

/**
 * Проверяет, что все документы текущего этапа подписаны.
 * Если документов нет — переход разрешён.
 */
@Component
public class StageDocumentsSignedRequirement implements StageTransitionRequirement {
    private final DocumentServiceClient documentServiceClient;

    public StageDocumentsSignedRequirement(DocumentServiceClient documentServiceClient) {
        this.documentServiceClient = documentServiceClient;
    }

    @Override
    public List<TransitionViolation> validate(Building building, UUID userId, StageTransition transition) {
        if (transition == null || !transition.canTransition()) return List.of();
        if (building.getCurrentStageId() == null) return List.of();

        String token = resolveBearerToken();
        List<DocumentServiceClient.DocumentInstanceDto> docs =
            documentServiceClient.listStageDocuments(building.getProjectId(), building.getCurrentStageId(), token);

        List<DocumentServiceClient.DocumentInstanceDto> stageDocs = docs.stream()
            .filter(doc -> doc.getGroup() == null
                || doc.getGroup().getType() == null
                || !"PHOTO_REPORTS".equalsIgnoreCase(doc.getGroup().getType()))
            .toList();

        if (stageDocs.isEmpty()) return List.of();

        boolean allSigned = stageDocs.stream()
            .allMatch(doc -> "SIGNED".equalsIgnoreCase(doc.getStatus()));
        if (allSigned) return List.of();

        String missing = stageDocs.stream()
            .filter(doc -> !"SIGNED".equalsIgnoreCase(doc.getStatus()))
            .map(doc -> doc.getStatus() == null ? "UNKNOWN" : doc.getStatus())
            .collect(Collectors.joining(", "));

        return List.of(new TransitionViolation(
            "DOCUMENTS_NOT_SIGNED",
            "Нельзя перейти на следующий этап: есть неподписанные документы (" + missing + ")"
        ));
    }

    private String resolveBearerToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getTokenValue();
        }
        return null;
    }
}
