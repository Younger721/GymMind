package com.gymmind.mcp.application;

import java.util.List;

/** MCP 客户端端口：agent 通过它发现并调用远程 MCP server 的工具（技能动态扩展） */
public interface McpClientService {

    /** 列出所有已启用远程 server 的可见工具（单个 server 失效不阻断整体） */
    List<RemoteMcpTool> listRemoteTools();

    /**
     * 调用远程工具并返回文本结果。
     *
     * @param server     server 名称（enabledTools 中 mcp:&lt;server&gt;:&lt;tool&gt; 的 server 段）
     * @param tool       工具名
     * @param queryInput 自然语言/关键词输入，包装为 {"query": ...} 参数
     */
    String callTool(String server, String tool, String queryInput);
}
