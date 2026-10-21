import type { HarnessProgressEvent } from '@/api/harness'

export interface ExecutionGraphNode extends HarnessProgressEvent {
  id: string
}

export interface ExecutionGraphEdge {
  id: string
  source: string
  target: string
  active: boolean
}

export function executionStepId(step: HarnessProgressEvent): string {
  const stageId = step.stage === 'tool_start' || step.stage === 'tool_result'
    ? 'tool_execution'
    : step.stage
  return `${step.pluginId}:${stageId}`
}

export function upsertExecutionStep(steps: HarnessProgressEvent[], event: HarnessProgressEvent) {
  const next = [...steps]
  const index = next.findIndex(step => executionStepId(step) === executionStepId(event))

  if (index < 0) next.push(event)
  else next[index] = event

  return next
}

export interface ExecutionHistoryMetadata {
  sources?: Array<{ documentName?: string; excerpt?: string; text?: string; documentVersion?: number; knowledgeType?: string; effectiveFrom?: string | null; effectiveTo?: string | null } | string>
  processSteps?: HarnessProgressEvent[]
  processExpanded?: boolean
  processStepExpanded?: Record<string, boolean>
  failed?: boolean
}

export function mergeExecutionHistory<T extends { role: string; content: string }>(
  serverMessages: T[],
  savedMessages: Array<{ role: string; content: string } & ExecutionHistoryMetadata>
): Array<T & ExecutionHistoryMetadata> {
  const available = savedMessages.map(message => ({ ...message }))

  return serverMessages.map(message => {
    const savedIndex = available.findIndex(item => item.role === message.role && item.content === message.content)
    if (savedIndex < 0) return message
    const [saved] = available.splice(savedIndex, 1)
    return { ...message, ...saved }
  })
}

export function projectExecutionGraph(
  steps: HarnessProgressEvent[] = [],
  isProcessing = true,
  failed = false
) {
  const nodes: ExecutionGraphNode[] = steps.map(step => {
    const stillActive = step.status === 'STARTED' || step.status === 'RUNNING'
    const status = !isProcessing && stillActive
      ? failed ? 'FAILED' : 'COMPLETED'
      : step.status
    const message = !isProcessing && stillActive && !failed
      ? step.message.replace(/^(正在|开始|已启动)\s*/, '')
      : step.message

    return { ...step, status, message, id: executionStepId(step) }
  })
  const edges: ExecutionGraphEdge[] = nodes.slice(1).map((target, index) => ({
    id: `${nodes[index].id}->${target.id}`,
    source: nodes[index].id,
    target: target.id,
    active: isProcessing && (target.status === 'STARTED' || target.status === 'RUNNING')
  }))

  return { nodes, edges }
}
