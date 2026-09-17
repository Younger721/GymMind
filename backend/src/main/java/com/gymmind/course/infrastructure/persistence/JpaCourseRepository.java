package com.gymmind.course.infrastructure.persistence;

import com.gymmind.course.domain.model.Course;
import com.gymmind.course.domain.repository.CourseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
class JpaCourseRepository implements CourseRepository {
    private final SpringDataCourseRepository delegate;

    JpaCourseRepository(SpringDataCourseRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public Course save(Course course) {
        return delegate.save(course);
    }

    @Override
    public Optional<Course> findByTenantIdAndId(Long tenantId, Long id) {
        return delegate.findByTenantIdAndId(tenantId, id);
    }

    @Override
    public List<Course> findAllByTenantId(Long tenantId) {
        return delegate.findAllByTenantId(tenantId);
    }
}
