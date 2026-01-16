package ru.domium.building.client;

import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentServiceClient {
    private final DiscoveryClient discoveryClient;
    private final RestTemplate restTemplate;

    public List<DocumentInstanceDto> listStageDocuments(UUID projectId, UUID stageId, String bearerToken) {
        String baseUrl = resolveBaseUrl();
        if (baseUrl == null) return List.of();
        String url = baseUrl + "/projects/" + projectId + "/documents?stage=" + stageId;

        HttpHeaders headers = new HttpHeaders();
        if (bearerToken != null && !bearerToken.isBlank()) {
            headers.setBearerAuth(bearerToken);
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<List<DocumentInstanceDto>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
            );
            return response.getBody() == null ? List.of() : response.getBody();
        } catch (RestClientException e) {
            log.warn("Failed to fetch documents for project {} stage {}: {}", projectId, stageId, e.getMessage());
            return List.of();
        }
    }

    private String resolveBaseUrl() {
        List<ServiceInstance> instances = discoveryClient.getInstances("documents");
        if (instances == null || instances.isEmpty()) {
            instances = discoveryClient.getInstances("document-service");
        }
        if (instances == null || instances.isEmpty()) return null;
        ServiceInstance instance = instances.getFirst();
        return "http://" + instance.getHost() + ":" + instance.getPort();
    }

    @Data
    public static class DocumentInstanceDto {
        private String status;
        private GroupInfo group;
    }

    @Data
    public static class GroupInfo {
        private String type;
    }
}
