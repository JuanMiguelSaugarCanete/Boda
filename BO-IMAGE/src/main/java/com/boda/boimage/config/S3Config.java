package com.boda.boimage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

    @Bean
    public AwsCredentialsProvider credentialsProvider(S3Properties properties) {
        if (StringUtils.hasText(properties.getAccessKey()) && StringUtils.hasText(properties.getSecretKey())) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey()));
        }
        // Sin credenciales estáticas: cadena por defecto de AWS (variables de entorno,
        // ~/.aws/credentials, rol IAM...). Se resuelve de forma perezosa en cada llamada.
        return DefaultCredentialsProvider.create();
    }

    @Bean
    public S3AsyncClient s3AsyncClient(S3Properties properties, AwsCredentialsProvider credentialsProvider) {
        return S3AsyncClient.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(credentialsProvider)
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(S3Properties properties, AwsCredentialsProvider credentialsProvider) {
        return S3Presigner.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(credentialsProvider)
                .build();
    }
}
