package com.gymmind.knowledge.infrastructure.storage;

import com.gymmind.knowledge.application.KnowledgeObjectStore;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Component
@ConditionalOnProperty(name = "gymmind.object-store", havingValue = "minio")
public class MinioKnowledgeObjectStore implements KnowledgeObjectStore {

    private final MinioClient client;
    private final String bucket;

    public MinioKnowledgeObjectStore(
            @Value("${gymmind.object-store.minio-endpoint:http://localhost:9000}") String endpoint,
            @Value("${gymmind.object-store.minio-access-key:minioadmin}") String accessKey,
            @Value("${gymmind.object-store.minio-secret-key:minioadmin}") String secretKey,
            @Value("${gymmind.object-store.minio-bucket:gymmind}") String bucket) {
        this.client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        this.bucket = bucket;
        ensureBucket();
    }

    @Override
    public void put(String key, byte[] content, String contentType) {
        try {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(new ByteArrayInputStream(content), content.length, -1)
                    .contentType(contentType == null ? "application/octet-stream" : contentType)
                    .build());
        } catch (Exception ex) {
            throw new IllegalStateException("failed to store object in MinIO", ex);
        }
    }

    @Override
    public byte[] read(String key) {
        try (InputStream stream = client.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build())) {
            return stream.readAllBytes();
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public void delete(String key) {
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (Exception ex) {
            throw new IllegalStateException("failed to delete object from MinIO", ex);
        }
    }

    private void ensureBucket() {
        try {
            if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception ex) {
            throw new IllegalStateException("failed to ensure MinIO bucket", ex);
        }
    }
}
