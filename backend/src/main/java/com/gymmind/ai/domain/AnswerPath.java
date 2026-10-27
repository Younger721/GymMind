package com.gymmind.ai.domain;

/** Harness 单次问答采用的回答路径。 */
public enum AnswerPath {
    /** 不检索、不强制业务工具 */
    DIRECT_LLM,
    /** 仅知识库检索后生成 */
    KNOWLEDGE_ONLY,
    /** 仅 Function Call（及最终生成） */
    TOOLS_ONLY,
    /** 知识库与业务工具均可参与 */
    HYBRID
}
