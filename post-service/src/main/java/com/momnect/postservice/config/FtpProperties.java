package com.momnect.postservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter @Setter
@ConfigurationProperties(prefix = "ftp")
public class FtpProperties {
    private String fileServerUrl;
    private String server;
    private int port;
    private String user;
    private String password;
    private String path;
    private int connectTimeoutMs = 5000;
    private int dataTimeoutMs = 60000;
}
