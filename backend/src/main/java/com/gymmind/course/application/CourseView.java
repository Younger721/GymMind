package com.gymmind.course.application;
import com.gymmind.course.domain.model.CourseStatus; import java.time.Instant;
public record CourseView(Long id,Long tenantId,Long coachId,String title,String type,Instant startsAt,Instant endsAt,int capacity,int reservedCount,String location,CourseStatus status,String tenantName,String coachName,String description) {
    public CourseView(Long id, Long tenantId, Long coachId, String title, String type,
                      Instant startsAt, Instant endsAt, int capacity, int reservedCount,
                      String location, CourseStatus status) {
        this(id, tenantId, coachId, title, type, startsAt, endsAt, capacity, reservedCount,
                location, status, null, null, null);
    }
}
