package com.client.lms.dashboard.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * REST clients for other services. The admin's JWT is forwarded on each call
 * so the other service can check the ADMIN role itself.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient classroomRestClient(ServiceClientProperties props, BearerTokenForwardingInterceptor interceptor) {
        return RestClient.builder()
                .baseUrl(props.classroomBaseUrl())
                .requestInterceptor(interceptor)
                .build();
    }

    @Bean
    public RestClient processingRestClient(ServiceClientProperties props, BearerTokenForwardingInterceptor interceptor) {
        return RestClient.builder()
                .baseUrl(props.processingBaseUrl())
                .requestInterceptor(interceptor)
                .build();
    }
}
