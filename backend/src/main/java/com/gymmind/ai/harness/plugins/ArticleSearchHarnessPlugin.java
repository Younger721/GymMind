package com.gymmind.ai.harness.plugins;

import com.gymmind.ai.domain.ContextSegment;
import com.gymmind.ai.harness.AiHarnessPlugin;
import com.gymmind.ai.harness.AiHarnessPluginDescriptor;
import com.gymmind.search.application.ArticleSearchService;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.stereotype.Component;

import java.util.List;

/** 文章元数据搜索插件 */
@Component
public class ArticleSearchHarnessPlugin implements AiHarnessPlugin {

    private static final AiHarnessPluginDescriptor DESCRIPTOR = new AiHarnessPluginDescriptor(
            "search_articles",
            "文章搜索",
            "搜索已导入的健身文章元数据",
            "SEARCH",
            "search:read",
            true,
            false);

    private final ArticleSearchService articles;

    public ArticleSearchHarnessPlugin(ArticleSearchService articles) {
        this.articles = articles;
    }

    @Override
    public AiHarnessPluginDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public void contribute(CurrentActor actor, String input, List<ContextSegment> context) {
        var hits = articles.search(actor, input);
        if (hits != null) {
            hits.forEach(a -> context.add(new ContextSegment(
                    "article:" + a.id(), a.title(), a.summary() == null ? a.url() : a.summary())));
        }
    }
}
