package com.gymmind.course.application;
import java.time.Instant;
public record CreateCourseCommand(Long tenantId,Long coachId,String title,String type,Instant startsAt,Instant endsAt,int capacity,String location) {}
