package com.momnect.postservice.config;

import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignMultipartConfig {

    @Bean
    public feign.Logger.Level feignLoggerLevel() {
        return feign.Logger.Level.FULL;
    }
}
