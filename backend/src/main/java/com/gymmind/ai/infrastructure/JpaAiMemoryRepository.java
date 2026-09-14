package com.gymmind.ai.infrastructure;
import com.gymmind.ai.application.*;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository public class JpaAiMemoryRepository implements AiMemoryRepository {
 private final SpringDataAiMemoryRepository delegate;
 public JpaAiMemoryRepository(SpringDataAiMemoryRepository delegate){this.delegate=delegate;}
 public void save(Long t,Long u,AiMemoryType type,String key,String value){var e=delegate.findByTenantIdAndUserIdAndTypeAndKey(t,u,type,key).orElseGet(()->AiMemoryEntity.create(t,u,type,key,value));e.value=value;delegate.save(e);}
 public List<AiMemoryEntry> findAll(Long t,Long u){return delegate.findAllByTenantIdAndUserId(t,u).stream().map(AiMemoryEntity::toEntry).toList();}
 public void delete(Long t,Long u,AiMemoryType type,String key){delegate.findByTenantIdAndUserIdAndTypeAndKey(t,u,type,key).ifPresent(delegate::delete);}
}
