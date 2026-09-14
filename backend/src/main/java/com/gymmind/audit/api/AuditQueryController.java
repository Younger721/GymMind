package com.gymmind.audit.api;

import com.gymmind.audit.application.AuditQueryService;
import com.gymmind.audit.domain.OperationAudit;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.security.CurrentActorProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audits")
public class AuditQueryController {
    private final AuditQueryService service;
    private final CurrentActorProvider actors;

    public AuditQueryController(AuditQueryService service, CurrentActorProvider actors) {
        this.service = service;
        this.actors = actors;
    }

    @GetMapping
    public ApiResponse<List<OperationAudit>> list() {
        return ApiResponse.success(service.list(actors.requireCurrent()));
    }
}
