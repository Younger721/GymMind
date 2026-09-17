package com.gymmind.mcp.application;

import com.gymmind.knowledge.application.KnowledgeSearchService;
import com.gymmind.search.application.ArticleSearchService;
import com.gymmind.search.application.ArticleView;
import com.gymmind.search.application.ImportArticleCommand;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DefaultMcpToolService implements McpToolService {

    private static final McpTool SEARCH = new McpTool("search_articles", "Search tenant article metadata", "search:read");
    private static final McpTool IMPORT = new McpTool("import_article", "Import a public article URL", "search:write");
    private static final McpTool KNOWLEDGE = new McpTool("knowledge_search", "Search tenant knowledge base", "knowledge:read");

    private final ArticleSearchService articles;
    private final KnowledgeSearchService knowledge;

    public DefaultMcpToolService(ArticleSearchService articles, KnowledgeSearchService knowledge) {
        this.articles = articles;
        this.knowledge = knowledge;
    }

    @Override
    public List<McpTool> list(CurrentActor actor) {
        requireTenant(actor);
        List<McpTool> tools = new ArrayList<>();
        if (actor.hasPermission(SEARCH.permission())) {
            tools.add(SEARCH);
        }
        if (actor.hasPermission(IMPORT.permission())) {
            tools.add(IMPORT);
        }
        if (actor.hasPermission(KNOWLEDGE.permission())) {
            tools.add(KNOWLEDGE);
        }
        return tools;
    }

    @Override
    public Object invoke(CurrentActor actor, String name, String input) {
        requireTenant(actor);
        if (SEARCH.name().equals(name)) {
            if (!actor.hasPermission(SEARCH.permission())) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            return articles.search(actor, input);
        }
        if (IMPORT.name().equals(name)) {
            if (!actor.hasPermission(IMPORT.permission())) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            return importArticle(actor, input);
        }
        if (KNOWLEDGE.name().equals(name)) {
            if (!actor.hasPermission(KNOWLEDGE.permission())) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            return knowledge.search(actor, input);
        }
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    private ArticleView importArticle(CurrentActor actor, String input) {
        Map<String, String> fields = parseFields(input);
        String url = fields.getOrDefault("url", input);
        String title = fields.getOrDefault("title", url);
        String summary = fields.getOrDefault("summary", "");
        return articles.importArticle(actor, new ImportArticleCommand(url, title, summary));
    }

    private static Map<String, String> parseFields(String input) {
        if (input == null || input.isBlank()) {
            return Map.of();
        }
        if (input.contains("url=")) {
            String[] parts = input.split("[;&]");
            java.util.HashMap<String, String> map = new java.util.HashMap<>();
            for (String part : parts) {
                int idx = part.indexOf('=');
                if (idx > 0) {
                    map.put(part.substring(0, idx).trim(), part.substring(idx + 1).trim());
                }
            }
            return map;
        }
        return Map.of("url", input.trim());
    }

    private static void requireTenant(CurrentActor actor) {
        if (actor == null || actor.tenantId() == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
