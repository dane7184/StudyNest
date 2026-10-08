package com.client.lms.dashboard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Base URLs of other services the dashboard reads stats from.
 */
@ConfigurationProperties(prefix = "lms.services")
public record ServiceClientProperties(
        String classroomBaseUrl,
        String processingBaseUrl
) {}
