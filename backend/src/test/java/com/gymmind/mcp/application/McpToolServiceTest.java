package com.gymmind.mcp.application;

import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.knowledge.application.KnowledgeSearchService;
import com.gymmind.search.application.ArticleSearchService;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class McpToolServiceTest {

    @Test
    void listsOnlyToolsAllowedByActor() {
        var articles = mock(ArticleSearchService.class);
        var knowledge = mock(KnowledgeSearchService.class);
        var service = new DefaultMcpToolService(articles, knowledge);
        var actor = actor(Set.of("search:read", "knowledge:read"));
        assertThat(service.list(actor).stream().map(McpTool::name))
                .containsExactly("search_articles", "knowledge_search");
    }

    @Test
    void invokesSearchThroughApplicationService() {
        var articles = mock(ArticleSearchService.class);
        var knowledge = mock(KnowledgeSearchService.class);
        when(articles.search(any(), eq("力量"))).thenReturn(java.util.List.of());
        var service = new DefaultMcpToolService(articles, knowledge);
        assertThat(service.invoke(actor(Set.of("search:read")), "search_articles", "力量")).isEqualTo(java.util.List.of());
        verify(articles).search(any(), eq("力量"));
    }

    @Test
    void deniesUnknownOrUnauthorizedTool() {
        var service = new DefaultMcpToolService(mock(ArticleSearchService.class), mock(KnowledgeSearchService.class));
        assertThatThrownBy(() -> service.invoke(actor(Set.of()), "search_articles", "x"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.invoke(actor(Set.of("search:read")), "missing", "x"))
                .isInstanceOf(BusinessException.class);
    }

    private CurrentActor actor(Set<String> permissions) {
        return new CurrentActor(1L, 7L, Set.of(RoleCode.GYM_ADMIN), permissions, 0, "t");
    }
}
