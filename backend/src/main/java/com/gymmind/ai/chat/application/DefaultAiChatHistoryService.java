package com.gymmind.ai.chat.application;
import com.gymmind.ai.chat.domain.model.*; import com.gymmind.ai.chat.infrastructure.*; import com.gymmind.shared.security.*; import com.gymmind.shared.error.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import java.util.*;
@Service public class DefaultAiChatHistoryService implements AiChatHistoryService{
 private final SpringDataAiChatSessionRepository sessions; private final SpringDataAiChatMessageRepository messages; private final AiRoleProfileResolver profiles;
 public DefaultAiChatHistoryService(SpringDataAiChatSessionRepository s,SpringDataAiChatMessageRepository m,AiRoleProfileResolver p){sessions=s;messages=m;profiles=p;}
 private String role(CurrentActor a){return profiles.resolve(a).roleCode();}
 /** 平台管理员的对话固定在平台身份下，不随顶栏门店变成门店问答记录。 */
 private Long scopeTenant(CurrentActor a){return a!=null&&a.isPlatformAdmin()?null:ActorAccess.tenantId(a);}
 private AiChatSession find(CurrentActor a,String k){Long t=scopeTenant(a); return (t==null?sessions.findByUserIdAndRoleContextAndTenantIdIsNullAndSessionKeyAndArchivedFalse(a.userId(),role(a),k):sessions.findByTenantIdAndUserIdAndRoleContextAndSessionKeyAndArchivedFalse(t,a.userId(),role(a),k)).orElseThrow(()->new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));}
 @Transactional public AiChatSession ensureSession(CurrentActor a,String k,String title){if(a==null||k==null||k.isBlank())throw new BusinessException(ErrorCode.VALIDATION_FAILED); Long t=scopeTenant(a); return (t==null?sessions.findByUserIdAndRoleContextAndTenantIdIsNullAndSessionKeyAndArchivedFalse(a.userId(),role(a),k):sessions.findByTenantIdAndUserIdAndRoleContextAndSessionKeyAndArchivedFalse(t,a.userId(),role(a),k)).orElseGet(()->sessions.save(new AiChatSession(t,a.userId(),role(a),k,title==null?"新建对话":title)));}
 @Transactional(readOnly=true) public List<AiChatSession> list(CurrentActor a){Long t=scopeTenant(a);return t==null?sessions.findTop30ByUserIdAndRoleContextAndTenantIdIsNullAndArchivedFalseOrderByUpdatedAtDesc(a.userId(),role(a)):sessions.findTop30ByTenantIdAndUserIdAndRoleContextAndArchivedFalseOrderByUpdatedAtDesc(t,a.userId(),role(a));}
 @Transactional(readOnly=true) public List<AiChatMessage> messages(CurrentActor a,String k){return messages.findTop40BySessionOrderBySequenceDesc(find(a,k)).stream().sorted(Comparator.comparingLong(AiChatMessage::getSequence)).toList();}
 @Transactional public void append(CurrentActor a,String k,AiChatMessageRole r,String c){if(c==null||c.isBlank()||c.length()>12000)throw new BusinessException(ErrorCode.VALIDATION_FAILED); AiChatSession s=ensureSession(a,k,c); long seq=messages.findTopBySessionOrderBySequenceDesc(s).map(x->x.getSequence()+1).orElse(1L); messages.save(new AiChatMessage(s,r,c.trim(),seq)); s.touch(c); sessions.save(s);}
 @Transactional public void archive(CurrentActor a,String k){AiChatSession s=find(a,k);s.archive();sessions.save(s);}
}
