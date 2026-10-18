package com.gymmind.course.domain.repository;
import com.gymmind.course.domain.model.Course;

import java.util.List;
import java.util.Optional;

public interface CourseRepository {
    Course save(Course course);
    boolean reserveCapacity(Long tenantId, Long courseId);
    boolean releaseCapacity(Long tenantId, Long courseId);
    Optional<Course> findById(Long id);
    Optional<Course> findByTenantIdAndId(Long tenantId, Long id);
    List<Course> findAllByTenantId(Long tenantId);
    List<Course> findAll();
}
