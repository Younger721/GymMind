package com.gymmind.ai.harness.plugins;

import com.gymmind.ai.domain.ContextSegment;
import com.gymmind.ai.harness.AiHarnessPlugin;
import com.gymmind.ai.harness.AiHarnessPluginDescriptor;
import com.gymmind.mcp.application.McpToolService;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.stereotype.Component;

import java.util.List;

/** 通过 MCP 风格调用导入文章（输入含 url 时触发） */
@Component
public class ImportArticleHarnessPlugin implements AiHarnessPlugin {

    private static final AiHarnessPluginDescriptor DESCRIPTOR = new AiHarnessPluginDescriptor(
            "import_article",
            "文章导入",
            "当问题包含 URL 时尝试导入公开文章",
            "SEARCH",
            "search:write",
            true,
            false);

    private final McpToolService mcp;

    public ImportArticleHarnessPlugin(McpToolService mcp) {
        this.mcp = mcp;
    }

    @Override
    public AiHarnessPluginDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public void contribute(CurrentActor actor, String input, List<ContextSegment> context) {
        if (input == null || !input.contains("http")) {
            return;
        }
        try {
            Object result = mcp.invoke(actor, "import_article", input);
            if (result != null) {
                context.add(new ContextSegment("tool:import_article", "import_article", result.toString()));
            }
        } catch (RuntimeException ignored) {
            // 导入失败不阻断对话
        }
    }
}
