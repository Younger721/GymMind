package com.gymmind.booking.application;

import com.gymmind.booking.domain.model.Booking;
import com.gymmind.booking.domain.model.BookingStatus;
import java.time.Instant;

public record BookingView(
        Long id,
        Long tenantId,
        Long memberId,
        String memberName,
        String memberPhone,
        Long courseId,
        BookingStatus status,
        Instant checkedInAt) {

    public BookingView(long id, long tenantId, long memberId, long courseId, BookingStatus status) {
        this(id, tenantId, memberId, null, null, courseId, status, null);
    }

    public BookingView(Long id, Long tenantId, Long memberId, Long courseId, BookingStatus status, Instant checkedInAt) {
        this(id, tenantId, memberId, null, null, courseId, status, checkedInAt);
    }

    public static BookingView from(Booking booking) {
        return new BookingView(booking.getId(), booking.getTenantId(), booking.getMemberId(), null, null,
                booking.getCourseId(), booking.getStatus(), booking.getCheckedInAt());
    }
}
