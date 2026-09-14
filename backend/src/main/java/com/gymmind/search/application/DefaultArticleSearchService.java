package com.gymmind.search.application;

import com.gymmind.search.domain.PublicUrlValidator;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class DefaultArticleSearchService implements ArticleSearchService {
    private final PublicUrlValidator urlValidator;
    private final AtomicLong ids = new AtomicLong(0);
    private final ConcurrentHashMap<Long, ArticleView> articles = new ConcurrentHashMap<>();

    public DefaultArticleSearchService(PublicUrlValidator urlValidator) {
        this.urlValidator = urlValidator;
    }

    @Override
    public ArticleView importArticle(CurrentActor actor, ImportArticleCommand command) {
        require(actor, "search:write");
        if (command == null || command.url() == null || command.url().isBlank()
                || command.title() == null || command.title().isBlank()
                || !urlValidator.isAllowed(command.url())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        var view = new ArticleView(ids.incrementAndGet(), actor.tenantId(), command.url().trim(),
                command.title().trim(), command.summary() == null ? "" : command.summary().trim());
        articles.put(view.id(), view);
        return view;
    }

    @Override
    public List<ArticleView> search(CurrentActor actor, String query) {
        require(actor, "search:read");
        if (query == null || query.isBlank()) throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        var needle = query.trim().toLowerCase(Locale.ROOT);
        return articles.values().stream()
                .filter(a -> a.tenantId() == actor.tenantId())
                .filter(a -> (a.title() + " " + a.summary() + " " + a.url()).toLowerCase(Locale.ROOT).contains(needle))
                .sorted(Comparator.comparingLong(ArticleView::id).reversed())
                .limit(20)
                .toList();
    }

    private static void require(CurrentActor actor, String permission) {
        if (actor == null || actor.tenantId() == null || !actor.hasPermission(permission)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
