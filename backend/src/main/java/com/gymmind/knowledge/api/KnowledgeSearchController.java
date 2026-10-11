package com.gymmind.knowledge.api;

import com.gymmind.ai.domain.ContextSegment;
import com.gymmind.knowledge.application.KnowledgeSearchDegradationContext;
import com.gymmind.knowledge.application.KnowledgeSearchService;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.security.CurrentActorProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/knowledge")
@SecurityRequirement(name = "bearerAuth")
public class KnowledgeSearchController {

    private final KnowledgeSearchService service;
    private final CurrentActorProvider actors;
    private final KnowledgeSearchDegradationContext degradationContext;

    public KnowledgeSearchController(
            KnowledgeSearchService service,
            CurrentActorProvider actors,
            KnowledgeSearchDegradationContext degradationContext) {
        this.service = service;
        this.actors = actors;
        this.degradationContext = degradationContext;
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ContextSegment>>> search(@RequestParam String q) {
        List<ContextSegment> segments = service.search(actors.requireCurrent(), q);
        return ResponseEntity.ok()
                .header(KnowledgeSearchDegradationContext.HEADER_NAME, degradationContext.headerValue())
                .body(ApiResponse.success(segments));
    }
}
