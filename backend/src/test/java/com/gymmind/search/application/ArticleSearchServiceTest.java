package com.gymmind.search.application;

import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.search.domain.PublicUrlValidator;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class ArticleSearchServiceTest {
    private final CurrentActor actor = new CurrentActor(1L, 7L, Set.of(RoleCode.GYM_ADMIN), Set.of("search:write", "search:read"), 0, "t");

    @Test
    void importsPublicArticleAndSearchesOnlyWithinTenant() {
        var service = new DefaultArticleSearchService(new PublicUrlValidator());
        var imported = service.importArticle(actor, new ImportArticleCommand("https://example.com/fitness", "训练基础", "力量训练要点"));

        assertThat(imported.url()).isEqualTo("https://example.com/fitness");
        assertThat(service.search(actor, "力量")).extracting(ArticleView::id).containsExactly(imported.id());
        var otherTenant = new CurrentActor(2L, 8L, Set.of(RoleCode.GYM_ADMIN), Set.of("search:read"), 0, "t2");
        assertThat(service.search(otherTenant, "力量")).isEmpty();
    }

    @Test
    void rejectsPrivateImportTarget() {
        var service = new DefaultArticleSearchService(new PublicUrlValidator());
        assertThatThrownBy(() -> service.importArticle(actor, new ImportArticleCommand("http://127.0.0.1/admin", "x", "y")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsPlatformActor() {
        var service = new DefaultArticleSearchService(new PublicUrlValidator());
        var platform = new CurrentActor(9L, null, Set.of(RoleCode.PLATFORM_ADMIN), Set.of("search:write"), 0, "p");
        assertThatThrownBy(() -> service.importArticle(platform, new ImportArticleCommand("https://example.com", "x", "y")))
                .isInstanceOf(BusinessException.class);
    }
}
