package com.gymmind.course.api;

import com.gymmind.course.application.CourseService;
import com.gymmind.course.application.CourseView;
import com.gymmind.course.application.CreateCourseCommand;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActorProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/courses")
@SecurityRequirement(name = "bearerAuth")
public class CourseController {

    private final CourseService service;
    private final CurrentActorProvider actors;

    public CourseController(CourseService service, CurrentActorProvider actors) {
        this.service = service;
        this.actors = actors;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CourseView> create(@Valid @RequestBody CreateRequest request) {
        return ApiResponse.success(service.create(actors.requireCurrent(),
                new CreateCourseCommand(request.tenantId(), request.coachId(), request.title(), request.type(),
                        request.startsAt(), request.endsAt(), request.capacity(), request.location(), request.description())));
    }

    @GetMapping
    public ApiResponse<List<CourseView>> list(@RequestParam(required = false) String from,
                                              @RequestParam(required = false) String to,
                                              @RequestParam(required = false) String type,
                                              @RequestParam(required = false) Long coachId,
                                              @RequestParam(defaultValue = "all") String scope,
                                              @RequestParam(defaultValue = "false") boolean availableOnly) {
        Instant fromInstant = parseDate(from, false);
        Instant toInstant = parseDate(to, true);
        return ApiResponse.success(service.list(actors.requireCurrent(), scope, fromInstant, toInstant, type, coachId, availableOnly));
    }

    @GetMapping("/{id}")
    public ApiResponse<CourseView> find(@PathVariable Long id) {
        return ApiResponse.success(service.find(actors.requireCurrent(), id));
    }

    @PatchMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody UpdateRequest request) {
        service.update(actors.requireCurrent(), id, request.title(), request.type(), request.startsAt(),
                request.endsAt(), request.capacity(), request.location(), request.description());
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        service.cancel(actors.requireCurrent(), id);
        return ApiResponse.success(null);
    }

    public record CreateRequest(Long tenantId, Long coachId, @NotBlank String title, @NotBlank String type,
                                Instant startsAt, Instant endsAt, int capacity, @NotBlank String location,
                                String description) {
    }

    public record UpdateRequest(@NotBlank String title, @NotBlank String type, Instant startsAt, Instant endsAt,
                                int capacity, @NotBlank String location, String description) {
    }

    private static Instant parseDate(String value, boolean endOfDay) {
        if (value == null || value.isBlank()) return null;
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            try {
                LocalDate date = LocalDate.parse(value);
                return date.plusDays(endOfDay ? 1 : 0).atStartOfDay(ZoneOffset.UTC).toInstant();
            } catch (DateTimeParseException invalidDate) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED);
            }
        }
    }
}
