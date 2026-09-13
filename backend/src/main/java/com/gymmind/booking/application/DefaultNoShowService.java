package com.gymmind.booking.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.audit.domain.AuditEvent;
import com.gymmind.audit.domain.AuditResult;
import com.gymmind.booking.domain.model.BookingStatus;
import com.gymmind.booking.domain.repository.BookingRepository;
import com.gymmind.course.domain.repository.CourseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
public class DefaultNoShowService implements NoShowService {
    private final BookingRepository bookings;
    private final CourseRepository courses;
    private final AuditRecorder audit;

    public DefaultNoShowService(BookingRepository bookings, CourseRepository courses, AuditRecorder audit) {
        this.bookings = bookings;
        this.courses = courses;
        this.audit = audit;
    }

    @Override
    @Transactional
    public int markTenantNoShows(Long tenantId, Instant now) {
        if (tenantId == null || tenantId <= 0 || now == null) {
            throw new IllegalArgumentException("Tenant context and clock are required");
        }
        int changed = 0;
        for (var booking : bookings.findAllByTenantIdAndStatus(tenantId, BookingStatus.CONFIRMED)) {
            var course = courses.findByTenantIdAndId(tenantId, booking.getCourseId()).orElse(null);
            if (course != null && !course.getEndsAt().isAfter(now)) {
                booking.markNoShow();
                bookings.save(booking);
                audit.record(new AuditEvent("BOOKING_NO_SHOW", "BOOKING", booking.getCourseId(),
                        AuditResult.SUCCESS, "no-show-scheduler", Map.of("source", "scheduler")));
                changed++;
            }
        }
        return changed;
    }
}
