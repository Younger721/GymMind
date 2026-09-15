package com.gymmind.ai.application;

import com.gymmind.analytics.application.DashboardQueryService;
import com.gymmind.analytics.domain.DashboardMetrics;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DefaultAiAnalysisService {
    private final DashboardQueryService dashboard;
    private final ChatModelGateway model;

    public DefaultAiAnalysisService(DashboardQueryService dashboard, ChatModelGateway model) {
        this.dashboard = dashboard;
        this.model = model;
    }

    public AiAnalysisResult memberAnalysis(CurrentActor actor, MemberAnalysisInput input) {
        require(actor);
        if (input == null || input.daysSinceLastWorkout() < 0 || input.workoutsLast30Days() < 0
                || input.workoutsPrevious30Days() < 0 || input.membershipDaysRemaining() < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        String prompt = "请分析会员训练活跃度并给出可执行建议。训练频率：最近30天="
                + input.workoutsLast30Days() + "，此前30天=" + input.workoutsPrevious30Days()
                + "；距上次训练=" + input.daysSinceLastWorkout() + "天；会员剩余="
                + input.membershipDaysRemaining() + "天；即将到期=" + input.membershipExpiring();
        return complete(prompt, "当前缺少足够会员数据，建议教练结合实际情况进行人工跟进。");
    }

    public AiAnalysisResult operationAnalysis(CurrentActor actor, LocalDate from, LocalDate to) {
        require(actor);
        if (from == null || to == null || to.isBefore(from)) throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        DashboardMetrics m = dashboard.dashboard(actor, from, to);
        String prompt = "请分析健身房运营数据并给出三条可执行建议：会员数=" + m.memberCount()
                + "，活跃会员数=" + m.activeMemberCount() + "，预约数=" + m.bookingCount()
                + "，签到数=" + m.checkInCount() + "，记录收入=" + m.recordedRevenue()
                + "，续费率=" + m.renewalRate();
        return complete(prompt, "当前运营数据不足，建议继续积累会员活跃、预约和续费记录。");
    }

    private AiAnalysisResult complete(String prompt, String fallback) {
        try {
            String answer = model.complete(prompt, List.of());
            return new AiAnalysisResult(answer == null || answer.isBlank() ? fallback : answer.trim());
        } catch (RuntimeException ex) {
            return new AiAnalysisResult(fallback);
        }
    }

    private static void require(CurrentActor actor) {
        if (actor == null || actor.tenantId() == null || actor.isPlatformAdmin()
                || !actor.hasPermission("ai:analysis")) throw new BusinessException(ErrorCode.FORBIDDEN);
    }
}
