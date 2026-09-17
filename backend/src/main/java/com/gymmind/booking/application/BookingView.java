package com.gymmind.booking.application;

import com.gymmind.booking.domain.model.Booking;
import com.gymmind.booking.domain.model.BookingStatus;

public record BookingView(
        Long id,
        Long tenantId,
        Long memberId,
        Long courseId,
        BookingStatus status) {

    public static BookingView from(Booking booking) {
        return new BookingView(
                booking.getId(),
                booking.getTenantId(),
                booking.getMemberId(),
                booking.getCourseId(),
                booking.getStatus());
    }
}
