package com.gymmind.member.api;

import com.gymmind.member.application.CreateMemberCommand;
import com.gymmind.member.application.DefaultMemberService;
import com.gymmind.member.application.MemberService;
import com.gymmind.member.application.MemberView;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.CurrentActorProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/members")
@SecurityRequirement(name = "bearerAuth")
public class MemberController {
    private final MemberService service;
    private final CurrentActorProvider actors;

    public MemberController(MemberService service, CurrentActorProvider actors) {
        this.service = service;
        this.actors = actors;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MemberView> create(@Valid @RequestBody CreateRequest request) {
        return ApiResponse.success(service.create(actors.requireCurrent(),
                new CreateMemberCommand(null, request.memberNumber(), request.fullName(), request.phone(), request.userId())));
    }

    @GetMapping("/{memberId}")
    public ApiResponse<MemberView> find(@PathVariable Long memberId) { return ApiResponse.success(service.find(actors.requireCurrent(), memberId)); }

    @GetMapping("/me")
    public ApiResponse<MemberView> me() { return ApiResponse.success(service.findSelf(actors.requireCurrent())); }

    @PatchMapping("/{memberId}")
    public ApiResponse<Void> update(@PathVariable Long memberId, @Valid @RequestBody UpdateRequest request) {
        service.update(actors.requireCurrent(), memberId, request.fullName(), request.phone());
        return ApiResponse.success(null);
    }

    @PostMapping("/{memberId}/suspend")
    public ApiResponse<Void> suspend(@PathVariable Long memberId) {
        service.suspend(actors.requireCurrent(), memberId);
        return ApiResponse.success(null);
    }

    public record CreateRequest(@NotBlank String memberNumber, @NotBlank String fullName, @NotBlank String phone, Long userId) {}
    public record UpdateRequest(@NotBlank String fullName, @NotBlank String phone) {}
}
