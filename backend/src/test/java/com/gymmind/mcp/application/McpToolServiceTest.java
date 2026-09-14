package com.gymmind.mcp.application;

import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.search.application.*;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class McpToolServiceTest {
    @Test void listsOnlyToolsAllowedByActor() {
        var articles = mock(ArticleSearchService.class);
        var service = new DefaultMcpToolService(articles);
        var actor = actor(Set.of("search:read"));
        assertThat(service.list(actor).stream().map(McpTool::name)).containsExactly("search_articles");
    }
    @Test void invokesSearchThroughApplicationService() {
        var articles = mock(ArticleSearchService.class);
        when(articles.search(any(), eq("力量"))).thenReturn(java.util.List.of());
        var service = new DefaultMcpToolService(articles);
        assertThat(service.invoke(actor(Set.of("search:read")), "search_articles", "力量")).isEmpty();
        verify(articles).search(any(), eq("力量"));
    }
    @Test void deniesUnknownOrUnauthorizedTool() {
        var service = new DefaultMcpToolService(mock(ArticleSearchService.class));
        assertThatThrownBy(() -> service.invoke(actor(Set.of()), "search_articles", "x")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.invoke(actor(Set.of("search:read")), "missing", "x")).isInstanceOf(BusinessException.class);
    }
    private CurrentActor actor(Set<String> permissions) { return new CurrentActor(1L, 7L, Set.of(RoleCode.GYM_ADMIN), permissions, 0, "t"); }
}
