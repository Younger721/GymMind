package com.gymmind.analytics.application;
import com.gymmind.ai.application.ChatModelGateway;
import com.gymmind.analytics.domain.WeeklyMetrics;
import com.gymmind.shared.error.*;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
@Service public class DefaultWeeklyReportService implements WeeklyReportService {
 private final WeeklyMetricsService weekly; private final ChatModelGateway model;
 public DefaultWeeklyReportService(WeeklyMetricsService weekly,ChatModelGateway model){this.weekly=weekly;this.model=model;}
 public WeeklyReport report(CurrentActor actor,LocalDate start){if(actor==null||actor.tenantId()==null||!actor.hasPermission("ai:report"))throw new BusinessException(ErrorCode.FORBIDDEN);if(start==null)throw new BusinessException(ErrorCode.VALIDATION_FAILED);WeeklyMetrics m=weekly.weekly(actor,start);var x=m.metrics();String prompt="请基于以下健身房周指标生成一句中文总结：会员数="+x.memberCount()+"，活跃会员数="+x.activeMemberCount()+"，预约数="+x.bookingCount()+"，签到数="+x.checkInCount();String summary;try{summary=model.complete(prompt,java.util.List.of());}catch(RuntimeException e){summary=null;}if(summary==null||summary.isBlank())summary="本周共 "+x.bookingCount()+" 次预约，完成 "+x.checkInCount()+" 次签到。";return new WeeklyReport(m.weekStart(),m.weekEnd(),summary.trim());}
}
