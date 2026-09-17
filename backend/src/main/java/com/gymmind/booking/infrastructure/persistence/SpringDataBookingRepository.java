package com.gymmind.booking.infrastructure.persistence; import com.gymmind.booking.domain.model.Booking; import com.gymmind.booking.domain.model.BookingStatus; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List; import java.util.Optional; interface SpringDataBookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByTenantIdAndId(Long tenantId, Long bookingId);

    Optional<Booking> findByTenantIdAndMemberIdAndCourseId(Long tenantId, Long memberId, Long courseId);

    List<Booking> findAllByTenantIdAndStatus(Long tenantId, BookingStatus status);

    List<Booking> findAllByTenantId(Long tenantId);
}
