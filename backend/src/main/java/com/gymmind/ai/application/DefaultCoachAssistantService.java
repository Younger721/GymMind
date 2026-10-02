package com.gymmind.ai.application;

import com.gymmind.coach.application.CoachAssignmentService;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.tenancy.application.TenantQuotaService;
import com.gymmind.tenancy.application.TenantQuotaServices;
import com.gymmind.workout.application.WorkoutHistoryQueryService;
import com.gymmind.workout.application.WorkoutHistorySummary;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DefaultCoachAssistantService {

    private final ChatModelGateway model;
    private final CoachAssignmentService assignments;
    private final TenantQuotaService quotas;
    private final WorkoutHistoryQueryService history;
    private final DefaultAiUsageService usage;

    public DefaultCoachAssistantService(ChatModelGateway model, CoachAssignmentService assignments) {
        this(model, assignments, TenantQuotaServices.unboundedForLegacyConstruction(), null, null);
    }

    public DefaultCoachAssistantService(ChatModelGateway model, CoachAssignmentService assignments,
                                        TenantQuotaService quotas) {
        this(model, assignments, quotas, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public DefaultCoachAssistantService(ChatModelGateway model, CoachAssignmentService assignments,
                                        TenantQuotaService quotas, WorkoutHistoryQueryService history) {
        this(model, assignments, quotas, history, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public DefaultCoachAssistantService(ChatModelGateway model, CoachAssignmentService assignments,
                                        TenantQuotaService quotas, WorkoutHistoryQueryService history,
                                        DefaultAiUsageService usage) {
        this.model = model;
        this.assignments = assignments;
        this.quotas = quotas == null ? TenantQuotaServices.unboundedForLegacyConstruction() : quotas;
        this.history = history;
        this.usage = usage;
    }

    public AiAnalysisResult assist(CurrentActor actor, Long memberId, String request) {
        if (ActorAccess.tenantId(actor) == null
                || (!actor.roles().contains(RoleCode.COACH) && !ActorAccess.canManageTenant(actor))
                || !actor.hasPermission("ai:analysis")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (memberId == null || memberId <= 0 || request == null || request.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (!assignments.canAccess(actor, memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        String prompt = "请作为健身教练助手，基于会员训练记录给出安全、渐进、可执行的建议。会员编号="
                + memberId + "；教练请求=" + request.trim();
        if (history != null) {
            WorkoutHistorySummary summary = history.summarize(ActorAccess.tenantId(actor), memberId,
                    LocalDate.now().minusWeeks(8), LocalDate.now(), 30);
            prompt += "\n请只使用以下已完成训练记录作为参考，不要编造记录：\n" + summary.promptText();
        }
        quotas.ensureAiChatAllowed(ActorAccess.tenantId(actor));
        ChatModelCallResult call;
        try {
            long started = System.currentTimeMillis();
            call = model.completeWithUsage(prompt, List.of());
            if (call == null) call = ChatModelCallResult.unavailable(model.complete(prompt, List.of()),
                    Math.max(0, System.currentTimeMillis() - started), "OK");
        } catch (RuntimeException ex) {
            return new AiAnalysisResult("模型暂时不可用，请结合会员实际情况人工评估。");
        }
        if (usage != null) {
            try {
                usage.record(actor, call, "coach-assistant");
            } catch (RuntimeException ignored) {
                // Usage persistence must not replace a generated recommendation.
            }
        }
        String answer = call.content();
        return new AiAnalysisResult(answer == null || answer.isBlank()
                ? "暂时无法生成建议，请结合会员实际情况人工评估。"
                : answer.trim());
    }
}
