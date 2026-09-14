package com.gymmind.ai.application;
import com.gymmind.shared.error.*;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.stereotype.Service;
import java.util.Comparator;
import java.util.List;
@Service public class DefaultAiMemoryService implements AiMemoryService {
    private final AiMemoryRepository repository;
    public DefaultAiMemoryService(AiMemoryRepository repository){this.repository=repository;}
    public void write(CurrentActor actor,AiMemoryType type,String key,String value){var s=requireScope(actor); repository.save(s.tenantId(),s.userId(),requireType(type),requireText(key),requireText(value));}
    public List<AiMemoryEntry> list(CurrentActor actor){var s=requireScope(actor); return repository.findAll(s.tenantId(),s.userId()).stream().sorted(Comparator.comparing(AiMemoryEntry::type).thenComparing(AiMemoryEntry::key)).toList();}
    public List<AiMemoryEntry> context(CurrentActor actor){if(actor==null||actor.tenantId()==null||!actor.hasPermission("ai:chat"))throw new BusinessException(ErrorCode.FORBIDDEN);return listWithScope(actor);}
    public void delete(CurrentActor actor,AiMemoryType type,String key){var s=requireScope(actor); repository.delete(s.tenantId(),s.userId(),requireType(type),requireText(key));}
    private static AiMemoryType requireType(AiMemoryType type){if(type==null)throw new BusinessException(ErrorCode.VALIDATION_FAILED);return type;}
    private static Scope requireScope(CurrentActor a){if(a==null||a.tenantId()==null||!a.hasPermission("ai:memory"))throw new BusinessException(ErrorCode.FORBIDDEN);return new Scope(a.tenantId(),a.userId());}
    private static String requireText(String v){if(v==null||v.isBlank())throw new BusinessException(ErrorCode.VALIDATION_FAILED);return v.trim();}
    private List<AiMemoryEntry> listWithScope(CurrentActor actor){var s=new Scope(actor.tenantId(),actor.userId());return repository.findAll(s.tenantId(),s.userId()).stream().sorted(Comparator.comparing(AiMemoryEntry::type).thenComparing(AiMemoryEntry::key)).toList();}
    private record Scope(Long tenantId,Long userId){}
}
