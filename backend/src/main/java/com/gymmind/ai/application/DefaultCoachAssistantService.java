package com.gymmind.ai.application;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.error.*;
import com.gymmind.shared.security.CurrentActor;
import java.util.List;
import org.springframework.stereotype.Service;
@Service public class DefaultCoachAssistantService {
    private final ChatModelGateway model;
    public DefaultCoachAssistantService(ChatModelGateway model) { this.model = model; }
    public AiAnalysisResult assist(CurrentActor actor, Long memberId, String request) {
        if (actor == null || actor.tenantId() == null || (!actor.roles().contains(RoleCode.COACH) && !actor.roles().contains(RoleCode.GYM_ADMIN)) || !actor.hasPermission("ai:analysis")) throw new BusinessException(ErrorCode.FORBIDDEN);
        if (memberId == null || memberId <= 0 || request == null || request.isBlank()) throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        String prompt = "请作为健身教练助手，基于会员训练记录设计安全、渐进的恢复方案。会员编号=" + memberId + "；教练请求=" + request.trim();
        try { String answer = model.complete(prompt, List.of()); return new AiAnalysisResult(answer == null || answer.isBlank() ? "暂时无法生成建议，请结合会员实际情况人工评估。" : answer.trim()); }
        catch (RuntimeException ex) { return new AiAnalysisResult("模型暂时不可用，请结合会员实际情况人工评估。"); }
    }
}
