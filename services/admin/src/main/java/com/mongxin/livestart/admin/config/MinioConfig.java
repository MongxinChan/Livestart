package com.mongxin.livestart.admin.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioConfig {

    private String endpoint;
    private String accessKey;
    private String secretKey;

    @Bean
    public MinioClient minioClient(MinioConfig minioConfig) {
        String endpoint = StringUtils.hasText(minioConfig.getEndpoint()) ? minioConfig.getEndpoint() : "http://127.0.0.1:1900";
        String accessKey = StringUtils.hasText(minioConfig.getAccessKey()) ? minioConfig.getAccessKey() : "minioadmin";
        String secretKey = StringUtils.hasText(minioConfig.getSecretKey()) ? minioConfig.getSecretKey() : "minioadmin";
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
