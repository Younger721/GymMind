package com.gymmind.course.domain.repository;
import com.gymmind.course.domain.model.Course;
import java.util.Optional;
public interface CourseRepository { Course save(Course course); Optional<Course> findByTenantIdAndId(Long tenantId,Long id); }
