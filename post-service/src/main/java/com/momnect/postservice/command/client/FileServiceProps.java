package com.momnect.postservice.command.client;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter @Setter
@Component
@ConfigurationProperties(prefix = "clients.file-service")
public class FileServiceProps {
    private String baseUrl;
}
