package com.momnect.postservice.config;

import com.momnect.postservice.config.props.ClientsProps;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(ClientsProps.class)
public class RestClientConfig {

    @Bean
    public RestClient fileServiceRestClient(ClientsProps props) {
        return RestClient.builder()
                .baseUrl(props.getFileService().getBaseUrl())
                .build();
    }
}
