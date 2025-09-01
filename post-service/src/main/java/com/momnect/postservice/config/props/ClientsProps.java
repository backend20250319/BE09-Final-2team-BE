package com.momnect.postservice.config.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clients")
@Getter @Setter
public class ClientsProps {

    private final FileService fileService = new FileService();

    @Getter @Setter
    public static class FileService {
        private String baseUrl;
    }
}
