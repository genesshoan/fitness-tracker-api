package dev.genesshoan.fitnesstrackerapi.infrastructure.storage.s3.config;

import java.net.URI;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

    private StaticCredentialsProvider credentials(StorageProperties sp) {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(sp.accessKey(), sp.secretKey()));
    }

    @Bean
    public S3Client s3Client(StorageProperties sp) {
        return S3Client.builder()
                .endpointOverride(URI.create(sp.endpoint()))
                .region(Region.of(sp.region()))
                .credentialsProvider(credentials(sp))
                .forcePathStyle(sp.pathStyle())
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(StorageProperties p) {
        return S3Presigner.builder()
                .endpointOverride(URI.create(p.endpoint()))
                .region(Region.of(p.region()))
                .credentialsProvider(credentials(p))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(p.pathStyle())
                        .build())
                .build();
    }
}
