package com.gymmind.search.application;

import com.gymmind.shared.security.CurrentActor;
import java.util.List;

public interface ArticleSearchService {
    ArticleView importArticle(CurrentActor actor, ImportArticleCommand command);
    List<ArticleView> search(CurrentActor actor, String query);
}
