package com.gymmind.search.api;

import com.gymmind.search.application.*;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.security.CurrentActorProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search/articles")
public class ArticleSearchController {
    private final ArticleSearchService service;
    private final CurrentActorProvider actors;

    public ArticleSearchController(ArticleSearchService service, CurrentActorProvider actors) {
        this.service = service;
        this.actors = actors;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ArticleView> importArticle(@Valid @RequestBody Request request) {
        return ApiResponse.success(service.importArticle(actors.requireCurrent(),
                new ImportArticleCommand(request.url(), request.title(), request.summary())));
    }

    @GetMapping
    public ApiResponse<List<ArticleView>> search(@RequestParam @NotBlank String q) {
        return ApiResponse.success(service.search(actors.requireCurrent(), q));
    }

    public record Request(@NotBlank String url, @NotBlank String title, String summary) { }
}
