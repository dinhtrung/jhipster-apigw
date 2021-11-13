package com.ft.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.utils.StringUtils;

@Configuration
@EnableConfigurationProperties(S3ClientConfigurationProperties.class)
public class S3ClientConfiguration {

    @Bean
    public S3AsyncClient s3client(S3ClientConfigurationProperties s3props, AwsCredentialsProvider credentialsProvider) {
        S3Configuration serviceConfiguration = S3Configuration
            .builder()
            .checksumValidationEnabled(true)
            .chunkedEncodingEnabled(true)
            .build();

        S3AsyncClientBuilder s3AsyncClientBuilder = S3AsyncClient
            .builder()
            .region(s3props.getRegion())
            .credentialsProvider(credentialsProvider)
            .serviceConfiguration(serviceConfiguration);

        if (s3props.getEndpoint() != null) {
            s3AsyncClientBuilder = s3AsyncClientBuilder.endpointOverride(s3props.getEndpoint());
        }

        return s3AsyncClientBuilder.build();
    }

    @Bean
    public AwsCredentialsProvider awsCredentialsProvider(S3ClientConfigurationProperties s3props) {
        if (StringUtils.isBlank(s3props.getAccessKeyId())) {
            return DefaultCredentialsProvider.create();
        } else {
            return () -> (AwsCredentials) AwsBasicCredentials.create(s3props.getAccessKeyId(), s3props.getSecretAccessKey());
        }
    }
}
