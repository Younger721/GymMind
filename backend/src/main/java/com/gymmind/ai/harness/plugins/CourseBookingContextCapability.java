package com.gymmind.ai.harness.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gymmind.ai.harness.capability.AiCapability;
import com.gymmind.ai.harness.capability.CapabilityDescriptor;
import com.gymmind.ai.harness.capability.ToolExecutionContext;
import com.gymmind.ai.harness.capability.ToolResult;
import com.gymmind.booking.domain.model.Booking;
import com.gymmind.booking.domain.repository.BookingRepository;
import com.gymmind.course.domain.model.Course;
import com.gymmind.course.domain.repository.CourseRepository;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.member.domain.repository.MemberRepository;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class CourseBookingContextCapability implements AiCapability {
    private static final CapabilityDescriptor DESCRIPTOR = new CapabilityDescriptor(
            "course_booking_context", "课程预约情况", "查询当前门店或会员的课程预约、取消和签到状态", "READ",
            "booking:read", true, Set.of(RoleCode.MEMBER, RoleCode.COACH, RoleCode.GYM_ADMIN), schema());

    private final BookingRepository bookings;
    private final CourseRepository courses;
    private final MemberRepository members;

    private static JsonNode schema() {
        ObjectNode schema = JsonNodeFactory.instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("from").put("type", "string").put("format", "date");
        properties.putObject("to").put("type", "string").put("format", "date");
        return schema;
    }

    public CourseBookingContextCapability(BookingRepository bookings, CourseRepository courses, MemberRepository members) {
        this.bookings = bookings;
        this.courses = courses;
        this.members = members;
    }

    @Override public CapabilityDescriptor descriptor() { return DESCRIPTOR; }

    @Override
    public ToolResult execute(CurrentActor actor, JsonNode arguments, ToolExecutionContext context) {
        Long tenantId = ActorAccess.tenantId(actor);
        List<Booking> scoped;
        if (actor.roles().contains(RoleCode.MEMBER)) {
            Long memberId = members.findByTenantIdAndUserId(tenantId, actor.userId())
                    .map(m -> m.getId()).orElseThrow(() -> new IllegalArgumentException("当前会员没有预约数据"));
            scoped = bookings.findAllByTenantId(tenantId).stream()
                    .filter(b -> memberId.equals(b.getMemberId())).limit(50).toList();
        } else {
            scoped = bookings.findAllByTenantId(tenantId).stream().limit(50).toList();
        }
        long completed = scoped.stream().filter(b -> b.getStatus() == com.gymmind.booking.domain.model.BookingStatus.COMPLETED).count();
        long cancelled = scoped.stream().filter(b -> b.getStatus() == com.gymmind.booking.domain.model.BookingStatus.CANCELLED).count();
        String text = "【课程预约情况】预约" + scoped.size() + "次，其中已签到" + completed + "次，已取消" + cancelled + "次";
        return ToolResult.context("course-bookings:" + tenantId, text);
    }
}
