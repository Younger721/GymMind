package com.gymmind.booking.infrastructure.persistence; import com.gymmind.booking.domain.model.Booking; import com.gymmind.booking.domain.model.BookingStatus; import com.gymmind.booking.domain.repository.BookingRepository; import org.springframework.stereotype.Repository; import java.util.List; import java.util.Optional; @Repository
class JpaBookingRepository implements BookingRepository {
    private final SpringDataBookingRepository delegate;

    JpaBookingRepository(SpringDataBookingRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public Booking save(Booking booking) {
        return delegate.save(booking);
    }

    @Override
    public Optional<Booking> findByTenantIdAndId(Long tenantId, Long bookingId) {
        return delegate.findByTenantIdAndId(tenantId, bookingId);
    }

    @Override
    public Optional<Booking> findByTenantIdAndMemberIdAndCourseId(Long tenantId, Long memberId, Long courseId) {
        return delegate.findByTenantIdAndMemberIdAndCourseId(tenantId, memberId, courseId);
    }

    @Override
    public List<Booking> findAllByTenantIdAndStatus(Long tenantId, BookingStatus status) {
        return delegate.findAllByTenantIdAndStatus(tenantId, status);
    }

    @Override
    public List<Booking> findAllByTenantId(Long tenantId) {
        return delegate.findAllByTenantId(tenantId);
    }
}
