package com.gymmind.shared.async;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.assertj.core.api.Assertions.*;
class PersistentJobQueueTest {
 @Test void retriesWithExponentialBackoffAndEventuallyCompletes(){var q=new InMemoryPersistentJobQueue();var id=q.enqueue("INGEST",7L);var j=q.claimNext(Instant.parse("2026-01-01T00:00:00Z"),Duration.ofMinutes(5)).orElseThrow();q.retry(id,"boom",Instant.parse("2026-01-01T00:00:00Z"));assertThat(q.find(id).attempts()).isEqualTo(1);assertThat(q.find(id).nextAttemptAt()).isEqualTo(Instant.parse("2026-01-01T00:00:30Z"));q.complete(id);assertThat(q.find(id).status()).isEqualTo(JobStatus.COMPLETED);}
 @Test void expiredLeaseIsRecoverable(){var q=new InMemoryPersistentJobQueue();var id=q.enqueue("INGEST",7L);q.claimNext(Instant.parse("2026-01-01T00:00:00Z"),Duration.ofMinutes(1));q.recoverExpired(Instant.parse("2026-01-01T00:02:00Z"));assertThat(q.find(id).status()).isEqualTo(JobStatus.PENDING);}
}
