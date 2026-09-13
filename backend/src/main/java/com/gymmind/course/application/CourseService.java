package com.gymmind.course.application;
import com.gymmind.shared.security.CurrentActor;
public interface CourseService { CourseView create(CurrentActor actor,CreateCourseCommand command); void update(CurrentActor actor,Long id,String title,String type,java.time.Instant startsAt,java.time.Instant endsAt,int capacity,String location); void cancel(CurrentActor actor,Long id); }
