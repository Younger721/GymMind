package com.gymmind.shared.async;
import java.time.Instant;
public record PersistentJob(Long id,String type,Long tenantId,JobStatus status,int attempts,Instant nextAttemptAt,Instant leaseUntil,String lastError) {}
