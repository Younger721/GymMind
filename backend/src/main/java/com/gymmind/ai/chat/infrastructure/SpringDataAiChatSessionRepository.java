package com.gymmind.ai.chat.infrastructure;
import com.gymmind.ai.chat.domain.model.AiChatSession; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface SpringDataAiChatSessionRepository extends JpaRepository<AiChatSession,Long>{
 Optional<AiChatSession> findByTenantIdAndUserIdAndRoleContextAndSessionKeyAndArchivedFalse(Long tenantId,Long userId,String role,String key);
 Optional<AiChatSession> findByUserIdAndRoleContextAndTenantIdIsNullAndSessionKeyAndArchivedFalse(Long userId,String role,String key);
 List<AiChatSession> findTop30ByTenantIdAndUserIdAndRoleContextAndArchivedFalseOrderByUpdatedAtDesc(Long tenantId,Long userId,String role);
 List<AiChatSession> findTop30ByUserIdAndRoleContextAndTenantIdIsNullAndArchivedFalseOrderByUpdatedAtDesc(Long userId,String role);
}
