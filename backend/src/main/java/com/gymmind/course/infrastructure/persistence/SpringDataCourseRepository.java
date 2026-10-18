package com.gymmind.course.infrastructure.persistence; import com.gymmind.course.domain.model.Course; import org.springframework.data.jpa.repository.JpaRepository; import org.springframework.data.jpa.repository.Modifying; import org.springframework.data.jpa.repository.Query; import org.springframework.data.repository.query.Param; import java.util.Optional; interface SpringDataCourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findByTenantIdAndId(Long tenantId, Long id);
    java.util.List<Course> findAllByTenantId(Long tenantId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Course c set c.reservedCount = c.reservedCount + 1, c.version = c.version + 1, c.updatedAt = CURRENT_TIMESTAMP "
            + "where c.tenantId = :tenantId and c.id = :courseId "
            + "and c.status = com.gymmind.course.domain.model.CourseStatus.SCHEDULED "
            + "and c.reservedCount < c.capacity")
    int reserveCapacity(@Param("tenantId") Long tenantId, @Param("courseId") Long courseId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Course c set c.reservedCount = c.reservedCount - 1, c.version = c.version + 1, c.updatedAt = CURRENT_TIMESTAMP "
            + "where c.tenantId = :tenantId and c.id = :courseId and c.reservedCount > 0")
    int releaseCapacity(@Param("tenantId") Long tenantId, @Param("courseId") Long courseId);
}
