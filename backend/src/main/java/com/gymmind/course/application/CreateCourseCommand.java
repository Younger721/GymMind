package com.gymmind.course.application;
import java.time.Instant;
public record CreateCourseCommand(Long tenantId,Long coachId,String title,String type,Instant startsAt,Instant endsAt,int capacity,String location,String description) {
    public CreateCourseCommand(Long tenantId, Long coachId, String title, String type,
                               Instant startsAt, Instant endsAt, int capacity, String location) {
        this(tenantId, coachId, title, type, startsAt, endsAt, capacity, location, null);
    }
}
