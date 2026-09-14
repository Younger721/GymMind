package com.gymmind.mcp.application;
import com.gymmind.search.application.ArticleView;
import com.gymmind.shared.security.CurrentActor;
import java.util.List;
public interface McpToolService { List<McpTool> list(CurrentActor actor); List<ArticleView> invoke(CurrentActor actor, String name, String input); }
