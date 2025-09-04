package com.momnect.postservice.command.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FileServerUrlCache {

    private final String baseUrl;

    public FileServerUrlCache(
            @Value("${clients.file-service.base-url}") String configured) {
        if (configured != null && configured.endsWith("/")) {
            configured = configured.substring(0, configured.length() - 1);
        }
        this.baseUrl = configured;
    }

    public String getBaseUrl() { return baseUrl; }

    public String get() { return baseUrl; }
}
