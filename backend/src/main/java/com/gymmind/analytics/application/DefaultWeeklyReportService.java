package com.gymmind.analytics.application;
import com.gymmind.ai.application.ChatModelGateway;
import com.gymmind.ai.application.ChatModelCallResult;
import com.gymmind.ai.application.DefaultAiUsageService;
import com.gymmind.analytics.domain.WeeklyMetrics;
import com.gymmind.shared.error.*;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.tenancy.application.TenantQuotaService;
import com.gymmind.tenancy.application.TenantQuotaServices;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
@Service public class DefaultWeeklyReportService implements WeeklyReportService {
 private final WeeklyMetricsService weekly; private final ChatModelGateway model; private final TenantQuotaService quotas; private final DefaultAiUsageService usage;
 public DefaultWeeklyReportService(WeeklyMetricsService weekly,ChatModelGateway model){this(weekly,model,TenantQuotaServices.unboundedForLegacyConstruction(), null);}
 public DefaultWeeklyReportService(WeeklyMetricsService weekly,ChatModelGateway model,TenantQuotaService quotas){this(weekly,model,quotas, null);}
 @org.springframework.beans.factory.annotation.Autowired public DefaultWeeklyReportService(WeeklyMetricsService weekly,ChatModelGateway model,TenantQuotaService quotas, DefaultAiUsageService usage){this.weekly=weekly;this.model=model;this.quotas=quotas==null?TenantQuotaServices.unboundedForLegacyConstruction():quotas;this.usage=usage;}
 public WeeklyReport report(CurrentActor actor,LocalDate start){if(ActorAccess.tenantId(actor)==null||!actor.hasPermission("ai:report"))throw new BusinessException(ErrorCode.FORBIDDEN);if(start==null)throw new BusinessException(ErrorCode.VALIDATION_FAILED);WeeklyMetrics m=weekly.weekly(actor,start);var x=m.metrics();quotas.ensureAiChatAllowed(ActorAccess.tenantId(actor));String prompt="请基于以下健身房周指标生成一句中文总结：会员数="+x.memberCount()+"，活跃会员数="+x.activeMemberCount()+"，预约数="+x.bookingCount()+"，签到数="+x.checkInCount();String summary;try{long started=System.currentTimeMillis();ChatModelCallResult call=model.completeWithUsage(prompt,java.util.List.of());if(call==null)call=ChatModelCallResult.unavailable(model.complete(prompt,java.util.List.of()),Math.max(0,System.currentTimeMillis()-started),"OK");if(usage!=null)usage.record(actor,call,"weekly-report");summary=call.content();}catch(RuntimeException e){summary=null;}if(summary==null||summary.isBlank())summary="本周共 "+x.bookingCount()+" 次预约，完成 "+x.checkInCount()+" 次签到。";return new WeeklyReport(m.weekStart(),m.weekEnd(),summary.trim());}
}
