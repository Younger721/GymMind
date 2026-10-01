package com.gymmind.analytics.application;
import com.gymmind.analytics.domain.*;
import com.gymmind.coach.application.CoachAssignmentService;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.error.*;
import com.gymmind.shared.security.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

@Service
public class DefaultMemberChurnRiskService implements MemberChurnRiskService {
 private final JdbcTemplate jdbc; private final CoachAssignmentService assignments; private final ChurnRiskCalculator calculator = new ChurnRiskCalculator();
 public DefaultMemberChurnRiskService(JdbcTemplate jdbc, CoachAssignmentService assignments){this.jdbc=jdbc;this.assignments=assignments;}
 public List<MemberChurnRiskView> list(CurrentActor actor){
  Long tenant=ActorAccess.tenantId(actor); if(tenant==null||!actor.hasPermission("analytics:read")) throw new BusinessException(ErrorCode.FORBIDDEN);
  Set<Long> allowed=null; if(actor.roles().contains(RoleCode.COACH)&&!ActorAccess.canManageTenant(actor)) allowed=new HashSet<>(assignments.list(actor, com.gymmind.coach.domain.model.CoachAssignmentStatus.ACTIVE).stream().map(a->a.getMemberId()).toList());
  Instant now=Instant.now(), from=now.minus(30, java.time.temporal.ChronoUnit.DAYS); List<Map<String,Object>> members=jdbc.queryForList("select id,full_name from member_profile where tenant_id=? and status='ACTIVE'",tenant); List<MemberChurnRiskView> out=new ArrayList<>();
  for(var m:members){Long id=((Number)m.get("id")).longValue();if(allowed!=null&&!allowed.contains(id))continue;var checks=jdbc.queryForList("select check_in_at from gym_check_in where tenant_id=? and member_id=? order by check_in_at desc",tenant,id);Instant last=checks.isEmpty()?null:(Instant)checks.get(0).get("check_in_at");int checkins=(int)checks.stream().filter(x->((Instant)x.get("check_in_at")).compareTo(from)>=0).count();Integer bookings=jdbc.queryForObject("select count(*) from course_booking where tenant_id=? and member_id=? and created_at>=? and status<>'CANCELLED'",Integer.class,tenant,id,from);Integer completed=jdbc.queryForObject("select count(*) from course_booking where tenant_id=? and member_id=? and checked_in_at>=? and status='COMPLETED'",Integer.class,tenant,id,from);Integer days=jdbc.queryForObject("select coalesce(datediff(expires_at,current_timestamp),9999) from member_membership where tenant_id=? and member_id=? and status='ACTIVE' order by expires_at limit 1",Integer.class,tenant,id);int remaining=days==null?9999:Math.max(days,0);int since=last==null?999:Math.max(0,(int)Duration.between(last,now).toDays());var risk=calculator.calculate(new ChurnSignals(since,checkins,0,remaining,remaining<=7));out.add(new MemberChurnRiskView(id,String.valueOf(m.get("full_name")),last,checkins,bookings==null?0:bookings,completed==null?0:completed,remaining,risk.level(),risk.score(),risk.reasons()));}
  return out.stream().sorted(Comparator.comparing(MemberChurnRiskView::score).reversed()).toList();
 }
}
