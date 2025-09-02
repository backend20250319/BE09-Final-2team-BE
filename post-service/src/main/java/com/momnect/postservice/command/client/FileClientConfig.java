package com.momnect.postservice.command.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class FileClientConfig {

    @Bean("fileRestClient")
    RestClient fileRestClient(
            RestClient.Builder builder,
            @Value("${clients.file-service.base-url}") String baseUrl
    ) {
        return builder.baseUrl(baseUrl).build();
    }
}
