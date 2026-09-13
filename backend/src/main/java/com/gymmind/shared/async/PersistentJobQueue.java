package com.gymmind.shared.async;
import java.time.*; import java.util.Optional;
public interface PersistentJobQueue { Long enqueue(String type,Long tenantId); Optional<PersistentJob> claimNext(Instant now,Duration lease); void advance(Long id); void retry(Long id,String error,Instant now); void complete(Long id); void recoverExpired(Instant now); PersistentJob find(Long id); }
