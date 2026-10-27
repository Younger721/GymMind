package com.gymmind.booking.api;

import com.gymmind.booking.application.AccessGateService;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.security.CurrentActorProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 外接闸机：拉取可通行名单，上报通行结果。不接收人脸照片。 */
@RestController
@RequestMapping("/api/v1/access")
@SecurityRequirement(name = "bearerAuth")
public class AccessGateController {

    private final AccessGateService service;
    private final CurrentActorProvider actors;

    public AccessGateController(AccessGateService service, CurrentActorProvider actors) {
        this.service = service;
        this.actors = actors;
    }

    @GetMapping("/whitelist")
    public ApiResponse<List<AccessGateService.AccessMember>> whitelist() {
        return ApiResponse.success(service.whitelist(actors.requireCurrent()));
    }

    @PostMapping("/passes")
    public ApiResponse<Map<String, Boolean>> pass(@Valid @RequestBody PassRequest request) {
        boolean recorded = service.recordPass(actors.requireCurrent(), request.memberNumber(), request.gateId());
        return ApiResponse.success(Map.of("recorded", recorded));
    }

    public record PassRequest(@NotBlank String memberNumber, @NotBlank String gateId) {}
}
