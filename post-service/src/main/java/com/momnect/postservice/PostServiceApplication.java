package com.momnect.postservice;

import com.momnect.postservice.config.FtpProperties;
import jakarta.servlet.MultipartConfigElement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.util.unit.DataSize;
import org.springframework.web.client.RestTemplate;

@EnableConfigurationProperties(FtpProperties.class)
@SpringBootApplication
@EnableFeignClients(basePackages = "com.momnect.postservice.command.client")
public class PostServiceApplication {

    @Value("${post.upload.max-size-bytes:10485760}")
    private long maxFileBytes;

    @Value("${post.upload.max-request-size-bytes:52428800}")
    private long maxRequestBytes;

    public static void main(String[] args) {
        SpringApplication.run(PostServiceApplication.class, args);
    }

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(DataSize.ofBytes(maxFileBytes));
        factory.setMaxRequestSize(DataSize.ofBytes(maxRequestBytes));
        return factory.createMultipartConfig();
    }
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
