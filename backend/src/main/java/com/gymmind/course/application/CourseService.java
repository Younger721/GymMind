package com.gymmind.course.application;
import com.gymmind.shared.security.CurrentActor;
import java.util.List;

public interface CourseService {
    CourseView create(CurrentActor actor, CreateCourseCommand command);
    CourseView find(CurrentActor actor, Long id);
    List<CourseView> list(CurrentActor actor);
    List<CourseView> list(CurrentActor actor, String scope, java.time.Instant from, java.time.Instant to,
                           String type, Long coachId, boolean availableOnly);
    default List<CourseView> list(CurrentActor actor, java.time.Instant from, java.time.Instant to,
                                  String type, Long coachId, boolean availableOnly) {
        return list(actor);
    }
    void update(CurrentActor actor, Long id, String title, String type, java.time.Instant startsAt, java.time.Instant endsAt, int capacity, String location);
    void cancel(CurrentActor actor, Long id);
}
