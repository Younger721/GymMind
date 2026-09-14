package com.gymmind.ai.infrastructure;
import com.gymmind.ai.application.*;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
@Component public class JpaAiUsageRepository implements AiUsageRepository {
    private final List<AiUsageRecord> records = new CopyOnWriteArrayList<>();
    public AiUsageRecord save(AiUsageRecord record){ records.add(record); return record; }
    public List<AiUsageRecord> findByTenant(Long tenantId){ return records.stream().filter(r -> tenantId.equals(r.tenantId())).toList(); }
}
