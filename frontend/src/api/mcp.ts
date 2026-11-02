import apiClient from './client'
import type { ApiResponse } from '@/types/api'

/** 进程内 MCP 工具（GET /v1/mcp/tools 返回项） */
export interface McpToolView {
  name: string
  description: string
  permission: string
  inputSchemaJson?: string
}

/** 远程 MCP server 技能（GET /v1/mcp/remote-tools 返回项） */
export interface RemoteMcpToolView {
  server: string
  name: string
  qualifiedName: string
  description: string
  inputSchemaJson?: string
}

export const mcpApi = {
  tools() {
    return apiClient.get<any, ApiResponse<McpToolView[]>>('/v1/mcp/tools')
  },
  invoke(body: { name: string; input: string }) {
    return apiClient.post<any, ApiResponse<unknown>>('/v1/mcp/invoke', body)
  },
  /** 远程 MCP server 的可用技能清单（供 Agent 技能配置勾选） */
  remoteTools() {
    return apiClient.get<any, ApiResponse<RemoteMcpToolView[]>>('/v1/mcp/remote-tools')
  }
}
