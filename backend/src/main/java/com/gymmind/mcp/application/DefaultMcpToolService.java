package com.gymmind.mcp.application;
import com.gymmind.search.application.*;
import com.gymmind.shared.error.*;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DefaultMcpToolService implements McpToolService {
    private static final McpTool SEARCH = new McpTool("search_articles", "Search tenant article metadata", "search:read");
    private final ArticleSearchService articles;
    public DefaultMcpToolService(ArticleSearchService articles) { this.articles = articles; }
    public List<McpTool> list(CurrentActor actor) { requireTenant(actor); return actor.hasPermission(SEARCH.permission()) ? List.of(SEARCH) : List.of(); }
    public List<ArticleView> invoke(CurrentActor actor, String name, String input) {
        requireTenant(actor);
        if (!SEARCH.name().equals(name) || !actor.hasPermission(SEARCH.permission())) throw new BusinessException(ErrorCode.FORBIDDEN);
        return articles.search(actor, input);
    }
    private static void requireTenant(CurrentActor actor) { if (actor == null || actor.tenantId() == null) throw new BusinessException(ErrorCode.FORBIDDEN); }
}
