package com.gymmind.ai.api;

import com.gymmind.ai.application.AiModelPricing;
import com.gymmind.ai.application.AiModelPricingService;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.security.CurrentActorProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/platform/ai-model-pricing")
@SecurityRequirement(name = "bearerAuth")
public class AiModelPricingController {
    private final AiModelPricingService service;
    private final CurrentActorProvider actors;

    public AiModelPricingController(AiModelPricingService service, CurrentActorProvider actors) {
        this.service = service;
        this.actors = actors;
    }

    @GetMapping
    public ApiResponse<List<AiModelPricing>> list() {
        return ApiResponse.success(service.list(actors.requireCurrent()));
    }

    @PutMapping
    public ApiResponse<AiModelPricing> upsert(@RequestBody AiModelPricingService.PricingCommand command) {
        return ApiResponse.success(service.upsert(actors.requireCurrent(), command));
    }
}
