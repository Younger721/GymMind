package com.gymmind.ai.api;
import com.gymmind.ai.application.*;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.security.CurrentActorProvider;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/ai")
public class CoachAssistantController {
    private final DefaultCoachAssistantService service; private final CurrentActorProvider actors;
    public CoachAssistantController(DefaultCoachAssistantService service, CurrentActorProvider actors){this.service=service;this.actors=actors;}
    @PostMapping("/coach-assistant")
    public ApiResponse<AiAnalysisResult> assist(@RequestBody Request request){return ApiResponse.success(service.assist(actors.requireCurrent(),request.memberId(),request.request()));}
    public record Request(Long memberId,String request){}
}
