package com.gymmind.membership.api;

import com.gymmind.membership.application.CreateMembershipChangeCommand;
import com.gymmind.membership.application.MembershipChangeRequestService;
import com.gymmind.membership.application.MembershipChangeRequestView;
import com.gymmind.membership.domain.model.MembershipChangeRequestStatus;
import com.gymmind.membership.domain.model.MembershipChangeType;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/membership-change-requests")
@SecurityRequirement(name = "bearerAuth")
public class MembershipChangeRequestController {

    private final MembershipChangeRequestService service;
    private final CurrentActorProvider actors;

    public MembershipChangeRequestController(MembershipChangeRequestService service,
                                             CurrentActorProvider actors) {
        this.service = service;
        this.actors = actors;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MembershipChangeRequestView> create(@Valid @RequestBody CreateRequest request) {
        return ApiResponse.success(service.create(actors.requireCurrent(), request.toCommand()));
    }

    @GetMapping
    public ApiResponse<List<MembershipChangeRequestView>> list(
            @RequestParam(required = false) MembershipChangeRequestStatus status) {
        return ApiResponse.success(service.list(actors.requireCurrent(), status));
    }

    @GetMapping("/{id}")
    public ApiResponse<MembershipChangeRequestView> find(@PathVariable Long id) {
        return ApiResponse.success(service.find(actors.requireCurrent(), id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<MembershipChangeRequestView> cancel(@PathVariable Long id) {
        return ApiResponse.success(service.cancel(actors.requireCurrent(), id));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<MembershipChangeRequestView> approve(@PathVariable Long id,
                                                            @Valid @RequestBody DecisionRequest request) {
        return ApiResponse.success(service.approve(actors.requireCurrent(), id, request.comment()));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<MembershipChangeRequestView> reject(@PathVariable Long id,
                                                           @Valid @RequestBody DecisionRequest request) {
        return ApiResponse.success(service.reject(actors.requireCurrent(), id, request.comment()));
    }

    public record CreateRequest(
            @NotNull MembershipChangeType type,
            @NotNull Long membershipId,
            LocalDate freezeStart,
            LocalDate freezeEnd,
            Long targetMemberId,
            String reason) {

        CreateMembershipChangeCommand toCommand() {
            return new CreateMembershipChangeCommand(type, membershipId, freezeStart, freezeEnd, targetMemberId, reason);
        }
    }

    public record DecisionRequest(String comment) {
    }
}
