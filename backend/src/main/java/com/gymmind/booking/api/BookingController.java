package com.gymmind.booking.api;

import com.gymmind.booking.application.BookCourseCommand;
import com.gymmind.booking.application.BookingService;
import com.gymmind.booking.application.BookingView;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.security.CurrentActorProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
@SecurityRequirement(name = "bearerAuth")
public class BookingController {

    private final BookingService service;
    private final CurrentActorProvider actors;

    public BookingController(BookingService service, CurrentActorProvider actors) {
        this.service = service;
        this.actors = actors;
    }

    @GetMapping
    public ApiResponse<java.util.List<BookingView>> list() {
        return ApiResponse.success(service.list(actors.requireCurrent()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BookingView> book(@Valid @RequestBody BookRequest request) {
        return ApiResponse.success(service.book(
                actors.requireCurrent(),
                new BookCourseCommand(null, request.memberId(), request.courseId())));
    }

    @PostMapping("/{bookingId}/cancel")
    public ApiResponse<BookingView> cancel(@PathVariable Long bookingId) {
        return ApiResponse.success(service.cancel(actors.requireCurrent(), bookingId));
    }

    @PostMapping("/{bookingId}/confirm")
    public ApiResponse<BookingView> confirm(@PathVariable Long bookingId) {
        return ApiResponse.success(service.confirm(actors.requireCurrent(), bookingId));
    }

    public record BookRequest(@NotNull Long memberId, @NotNull Long courseId) {
    }
}
