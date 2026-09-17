package com.gymmind.course.application;
import com.gymmind.shared.security.CurrentActor;
import java.util.List;

public interface CourseService {
    CourseView create(CurrentActor actor, CreateCourseCommand command);
    CourseView find(CurrentActor actor, Long id);
    List<CourseView> list(CurrentActor actor);
    void update(CurrentActor actor, Long id, String title, String type, java.time.Instant startsAt, java.time.Instant endsAt, int capacity, String location);
    void cancel(CurrentActor actor, Long id);
}
