package com.english.content_service.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.english.service.FileService;

@Configuration
public class StoreConfiguration {
    @Value("${cloudinary.cloud_name}")
    private String cloudName;

    @Value("${cloudinary.api_key}")
    private String apiKey;

    @Value("${cloudinary.api_secret}")
    private String apiSecret;
    @Bean
    public FileService fileService() {
        return new FileService(cloudName,apiKey,apiSecret);
    }


}
