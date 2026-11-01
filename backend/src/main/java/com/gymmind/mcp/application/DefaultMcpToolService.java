package com.gymmind.mcp.application;

import com.gymmind.knowledge.application.KnowledgeSearchService;
import com.gymmind.search.application.ArticleSearchService;
import com.gymmind.search.application.ArticleView;
import com.gymmind.search.application.ImportArticleCommand;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.ActorAccess;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DefaultMcpToolService implements McpToolService {

    /** 各工具入参 JSON Schema：MCP tools/list 使用，与工具层 String input 约定对齐 */
    private static final String SEARCH_SCHEMA =
            "{\"type\":\"object\",\"properties\":{\"query\":{\"type\":\"string\",\"description\":\"检索关键词\"}},\"required\":[\"query\"]}";
    private static final String IMPORT_SCHEMA =
            "{\"type\":\"object\",\"properties\":{\"url\":{\"type\":\"string\",\"description\":\"文章 URL\"},"
                    + "\"title\":{\"type\":\"string\"},\"summary\":{\"type\":\"string\"}},\"required\":[\"url\"]}";

    private static final McpTool SEARCH = new McpTool("search_articles", "按关键词检索本租户已导入的文章元数据", "search:read", SEARCH_SCHEMA);
    private static final McpTool IMPORT = new McpTool("import_article", "导入一篇公开文章 URL 入库", "search:write", IMPORT_SCHEMA);
    private static final McpTool KNOWLEDGE = new McpTool("knowledge_search", "检索本租户知识库（价目、规章、健身知识），返回带引用的片段", "knowledge:read", SEARCH_SCHEMA);

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
        if (hasExplicitPermission(actor, SEARCH.permission())) {
            tools.add(SEARCH);
        }
        if (hasExplicitPermission(actor, IMPORT.permission())) {
            tools.add(IMPORT);
        }
        if (hasExplicitPermission(actor, KNOWLEDGE.permission())) {
            tools.add(KNOWLEDGE);
        }
        return tools;
    }

    @Override
    public Object invoke(CurrentActor actor, String name, String input) {
        requireTenant(actor);
        if (SEARCH.name().equals(name)) {
            if (!hasExplicitPermission(actor, SEARCH.permission())) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            return articles.search(actor, input);
        }
        if (IMPORT.name().equals(name)) {
            if (!hasExplicitPermission(actor, IMPORT.permission())) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            return importArticle(actor, input);
        }
        if (KNOWLEDGE.name().equals(name)) {
            if (!hasExplicitPermission(actor, KNOWLEDGE.permission())) {
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
        ActorAccess.requireTenantId(actor);
    }

    private static boolean hasExplicitPermission(CurrentActor actor, String permission) {
        return actor != null && permission != null && actor.hasPermission(permission);
    }
}
