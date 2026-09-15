package com.gymmind.ai.api;

import com.gymmind.ai.application.*;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.security.CurrentActorProvider;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
public class AiAnalysisController {
    private final DefaultAiAnalysisService service;
    private final CurrentActorProvider actors;

    public AiAnalysisController(DefaultAiAnalysisService service, CurrentActorProvider actors) {
        this.service = service;
        this.actors = actors;
    }

    @PostMapping("/member-analysis")
    public ApiResponse<AiAnalysisResult> member(@RequestBody MemberAnalysisInput request) {
        return ApiResponse.success(service.memberAnalysis(actors.requireCurrent(), request));
    }

    @PostMapping("/operation-analysis")
    public ApiResponse<AiAnalysisResult> operation(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success(service.operationAnalysis(actors.requireCurrent(), from, to));
    }
}
