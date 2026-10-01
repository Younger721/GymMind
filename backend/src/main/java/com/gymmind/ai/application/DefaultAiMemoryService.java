package com.gymmind.ai.application;
import com.gymmind.shared.error.*;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.tenancy.application.TenantQuotaService;
import org.springframework.stereotype.Service;
import java.util.Comparator;
import java.util.List;
@Service public class DefaultAiMemoryService implements AiMemoryService {
    private final AiMemoryRepository repository;
    private final TenantQuotaService quotas;
    public DefaultAiMemoryService(AiMemoryRepository repository, TenantQuotaService quotas){this.repository=repository;this.quotas=quotas;}
    public void write(CurrentActor actor,AiMemoryType type,String key,String value){var s=requireScope(actor); ensureModule(s.tenantId()); repository.save(s.tenantId(),s.userId(),requireType(type),requireText(key),requireText(value));}
    public List<AiMemoryEntry> list(CurrentActor actor){var s=requireScope(actor); ensureModule(s.tenantId()); return repository.findAll(s.tenantId(),s.userId()).stream().sorted(Comparator.comparing(AiMemoryEntry::type).thenComparing(AiMemoryEntry::key)).toList();}
    public List<AiMemoryEntry> context(CurrentActor actor){Long tenantId=ActorAccess.tenantId(actor);if(tenantId==null||!actor.hasPermission("ai:chat"))throw new BusinessException(ErrorCode.FORBIDDEN);ensureModule(tenantId);return listWithScope(actor);}
    public void delete(CurrentActor actor,AiMemoryType type,String key){var s=requireScope(actor); ensureModule(s.tenantId()); repository.delete(s.tenantId(),s.userId(),requireType(type),requireText(key));}
    private void ensureModule(Long tenantId){quotas.ensureAiModuleEnabled(tenantId);}
    private static AiMemoryType requireType(AiMemoryType type){if(type==null)throw new BusinessException(ErrorCode.VALIDATION_FAILED);return type;}
    private static Scope requireScope(CurrentActor a){Long tenantId=ActorAccess.tenantId(a);if(tenantId==null||!a.hasPermission("ai:memory"))throw new BusinessException(ErrorCode.FORBIDDEN);return new Scope(tenantId,a.userId());}
    private static String requireText(String v){if(v==null||v.isBlank())throw new BusinessException(ErrorCode.VALIDATION_FAILED);return v.trim();}
    private List<AiMemoryEntry> listWithScope(CurrentActor actor){var s=new Scope(ActorAccess.tenantId(actor),actor.userId());return repository.findAll(s.tenantId(),s.userId()).stream().sorted(Comparator.comparing(AiMemoryEntry::type).thenComparing(AiMemoryEntry::key)).toList();}
    private record Scope(Long tenantId,Long userId){}
}
