package com.gymmind.knowledge.infrastructure.storage;

import com.gymmind.knowledge.application.KnowledgeObjectStore;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.io.ByteArrayInputStream;

@Component
@ConditionalOnProperty(name = "gymmind.object-store", havingValue = "minio")
public class MinioKnowledgeObjectStore implements KnowledgeObjectStore {
    private final MinioClient client;
    private final String bucket;
    public MinioKnowledgeObjectStore() {
        String endpoint = env("MINIO_ENDPOINT", "http://localhost:9000");
        this.bucket = env("MINIO_BUCKET", "gymmind");
        this.client = MinioClient.builder().endpoint(endpoint)
                .credentials(env("MINIO_ACCESS_KEY", "minioadmin"), env("MINIO_SECRET_KEY", "minioadmin")).build();
    }
    public void put(String key, byte[] content, String contentType) {
        try { client.putObject(PutObjectArgs.builder().bucket(bucket).object(key).contentType(contentType)
                .stream(new ByteArrayInputStream(content), content.length, -1).build()); }
        catch (Exception e) { throw new IllegalStateException("object storage put failed", e); }
    }
    public void delete(String key) {
        try { client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build()); }
        catch (Exception e) { throw new IllegalStateException("object storage delete failed", e); }
    }
    private static String env(String n,String f){String v=System.getenv(n);return v==null||v.isBlank()?f:v;}
}
