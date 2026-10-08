package com.client.lms.dashboard.integration.client;

import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Calls content-processing-service (Python), which owns processing_jobs.
 */
@Component
public class ProcessingStatsClient {

    private final RestClient client;

    public ProcessingStatsClient(RestClient processingRestClient) {
        this.client = processingRestClient;
    }

    /** GET /internal/admin/jobs/stats -> {"QUEUED": 3, "RUNNING": 1, "DONE": 120, "FAILED": 2} */
    public Map<String, Long> jobsByStatus() {
        return client.get()
                .uri("/internal/admin/jobs/stats")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
