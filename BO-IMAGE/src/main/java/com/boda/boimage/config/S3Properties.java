package com.boda.boimage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "aws")
public class S3Properties {

    private String bucket;
    private String region = "eu-west-1";
    private String accessKey;
    private String secretKey;
    private long presignExpirationMinutes = 15;
}
