package com.zynboot.infra.storage.config;

import com.zynboot.infra.storage.config.StorageType;
import com.zynboot.infra.storage.service.StorageService;
import com.zynboot.infra.storage.service.impl.DefaultStorageService;
import com.zynboot.infra.storage.spi.StorageBackend;
import com.zynboot.infra.storage.spi.impl.LocalStorageBackend;
import com.zynboot.infra.storage.spi.impl.S3StorageBackend;
import com.zynboot.infra.storage.support.StorageObjectKeyGenerator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.io.IOException;
import java.net.URI;

@AutoConfiguration
@ConditionalOnProperty(prefix = "zyn.storage", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(StorageProperties.class)
public class StorageAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public StorageObjectKeyGenerator storageObjectKeyGenerator(StorageProperties properties) {
        properties.validate();
        return new StorageObjectKeyGenerator(properties.getDatePathPattern(), properties.getFilenameStrategy());
    }

    @Bean
    @ConditionalOnMissingBean(StorageService.class)
    public StorageService storageService(ObjectProvider<StorageBackend> storageBackendProvider,
                                         @Qualifier("storageS3Client") ObjectProvider<S3Client> s3ClientProvider,
                                         StorageObjectKeyGenerator storageObjectKeyGenerator,
                                         StorageProperties properties) throws IOException {
        StorageBackend storageBackend = storageBackendProvider.getIfAvailable();
        if (storageBackend == null) {
            storageBackend = createStorageBackend(properties, s3ClientProvider);
        }
        return new DefaultStorageService(
                storageBackend,
                storageObjectKeyGenerator,
                properties.getConflictStrategy(),
                properties.getMaxInMemoryReadBytes()
        );
    }

    private StorageBackend createStorageBackend(StorageProperties properties,
                                                ObjectProvider<S3Client> s3ClientProvider) throws IOException {
        properties.validate();
        if (properties.getType() == StorageType.LOCAL) {
            return new LocalStorageBackend(properties);
        }
        return createS3StorageBackend(properties, s3ClientProvider);
    }

    private StorageBackend createS3StorageBackend(StorageProperties properties, ObjectProvider<S3Client> s3ClientProvider) {
        S3Client existingClient = s3ClientProvider.getIfAvailable();
        if (existingClient != null) {
            return new S3StorageBackend(properties, existingClient, false);
        }
        throw new IllegalStateException("No storageS3Client bean available for S3 storage backend. "
                + "Configure S3 credentials via zyn.infra.storage.s3.access-key/secret-key, "
                + "or provide a custom S3Client bean named 'storageS3Client'.");
    }

    @Bean("storageS3Client")
    @ConditionalOnProperty(prefix = "zyn.storage", name = "type", havingValue = "S3")
    @ConditionalOnMissingBean(name = "storageS3Client")
    public S3Client storageS3Client(StorageProperties properties) {
        properties.validate();
        return buildS3Client(properties.getS3());
    }

    private S3Client buildS3Client(StorageProperties.S3Properties s3Properties) {
        String regionName = StringUtils.hasText(s3Properties.getRegion()) ? s3Properties.getRegion().trim() : "cn-north-1";
        var builder = S3Client.builder().region(Region.of(regionName));

        if (StringUtils.hasText(s3Properties.getAccessKey()) || StringUtils.hasText(s3Properties.getSecretKey())) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(s3Properties.getAccessKey().trim(), s3Properties.getSecretKey().trim())
            ));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        if (StringUtils.hasText(s3Properties.getEndpoint())) {
            builder.endpointOverride(URI.create(s3Properties.getEndpoint().trim()));
            builder.serviceConfiguration(S3Configuration.builder()
                    .pathStyleAccessEnabled(s3Properties.isPathStyleAccess())
                    .build());
        }

        return builder.build();
    }
}
