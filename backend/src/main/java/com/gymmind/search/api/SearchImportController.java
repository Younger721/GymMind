package com.gymmind.search.api;

import com.gymmind.search.application.ArticleSearchService;
import com.gymmind.search.application.ArticleView;
import com.gymmind.search.application.ImportArticleCommand;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.security.CurrentActorProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
public class SearchImportController {

    private final ArticleSearchService service;
    private final CurrentActorProvider actors;

    public SearchImportController(ArticleSearchService service, CurrentActorProvider actors) {
        this.service = service;
        this.actors = actors;
    }

    @PostMapping("/import")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ArticleView> importArticle(@Valid @RequestBody Request request) {
        return ApiResponse.success(service.importArticle(actors.requireCurrent(),
                new ImportArticleCommand(request.url(), request.title(), request.summary())));
    }

    public record Request(@NotBlank String url, @NotBlank String title, String summary) {
    }
}
