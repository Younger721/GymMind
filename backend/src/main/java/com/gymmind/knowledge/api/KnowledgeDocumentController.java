package com.gymmind.knowledge.api;

import com.gymmind.knowledge.application.KnowledgeDocumentUseCase;
import com.gymmind.knowledge.application.KnowledgeDocumentView;
import com.gymmind.knowledge.application.UpdateKnowledgeDocumentCommand;
import com.gymmind.knowledge.application.UploadKnowledgeDocumentCommand;
import com.gymmind.knowledge.domain.model.DocumentVisibility;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.security.CurrentActorProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/knowledge/documents")
@SecurityRequirement(name = "bearerAuth")
public class KnowledgeDocumentController {
    private final KnowledgeDocumentUseCase useCase;
    private final CurrentActorProvider actors;

    public KnowledgeDocumentController(KnowledgeDocumentUseCase useCase, CurrentActorProvider actors) {
        this.useCase = useCase;
        this.actors = actors;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<KnowledgeDocumentView> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam DocumentVisibility visibility) throws IOException {
        return ApiResponse.success(useCase.upload(actors.requireCurrent(),
                new UploadKnowledgeDocumentCommand(file.getOriginalFilename(), file.getContentType(),
                        file.getBytes(), visibility)));
    }

    @GetMapping
    public ApiResponse<List<KnowledgeDocumentView>> list() {
        return ApiResponse.success(useCase.list(actors.requireCurrent()));
    }

    @GetMapping("/{id}")
    public ApiResponse<KnowledgeDocumentView> find(@PathVariable Long id) {
        return ApiResponse.success(useCase.find(actors.requireCurrent(), id));
    }

    @PatchMapping("/{id}")
    public ApiResponse<KnowledgeDocumentView> update(
            @PathVariable Long id,
            @RequestParam DocumentVisibility visibility) {
        return ApiResponse.success(useCase.update(
                actors.requireCurrent(), id, new UpdateKnowledgeDocumentCommand(visibility)));
    }

    @PostMapping("/{id}/reindex")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<KnowledgeDocumentView> reindex(@PathVariable Long id) {
        return ApiResponse.success(useCase.reindex(actors.requireCurrent(), id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        useCase.delete(actors.requireCurrent(), id);
    }
}
