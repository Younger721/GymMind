package com.gymmind.booking.domain.repository; import com.gymmind.booking.domain.model.Booking; import com.gymmind.booking.domain.model.BookingStatus; import java.util.List; import java.util.Optional; public interface BookingRepository {
    Booking save(Booking booking);

    Optional<Booking> findByTenantIdAndId(Long tenantId, Long bookingId);

    Optional<Booking> findByTenantIdAndMemberIdAndCourseId(Long tenantId, Long memberId, Long courseId);

    List<Booking> findAllByTenantIdAndStatus(Long tenantId, BookingStatus status);

    List<Booking> findAllByTenantId(Long tenantId);
}
