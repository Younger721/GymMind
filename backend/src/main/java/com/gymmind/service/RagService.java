package com.gymmind.service;

import com.gymmind.dto.profile.UserProfileResponse;
import com.gymmind.dto.rag.*;
import com.gymmind.entity.KnowledgeChunk;
import com.gymmind.repository.KnowledgeChunkRepository;
import com.gymmind.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    // Cache keys are built from userId + query hash for uniqueness

    private final HybridRetrievalService hybridRetrievalService;
    private final UserProfileService userProfileService;
    private final KnowledgeChunkRepository chunkRepository;

    @Value("${ai.openai.api-key}")
    private String apiKey;

    @Value("${ai.openai.base-url}")
    private String baseUrl;

    @Value("${ai.openai.chat.model}")
    private String model;

    @org.springframework.cache.annotation.Cacheable(
        value = "rag:queries",
        key = "#request.message",
        condition = "#request.history == null || #request.history.isEmpty()"
    )
    public ChatResponse chat(ChatRequest request) {
        long startTime = System.currentTimeMillis();
        Long userId = SecurityUtils.getCurrentUserId();

        log.info("RAG chat request from userId={}, message={}", userId, request.getMessage());

        // Get user profile for context
        UserProfileResponse profile = null;
        try {
            profile = userProfileService.getProfile();
        } catch (Exception e) {
            log.warn("Could not load user profile: {}", e.getMessage());
        }

        // Hybrid retrieval
        List<RetrievalResult> retrievalResults = hybridRetrievalService.hybridSearch(userId, request.getMessage());

        // Enrich with chunk text from database
        enrichRetrievalResults(retrievalResults);

        // Build context from retrieval results
        String context = buildContext(retrievalResults);

        // Build system prompt
        String systemPrompt = buildSystemPrompt(profile, context, !retrievalResults.isEmpty());

        // Generate answer
        String answer = generateAnswer(systemPrompt, request.getMessage(), request.getHistory());

        // Build source references
        List<SourceReference> sources = buildSourceReferences(retrievalResults);

        long responseTime = System.currentTimeMillis() - startTime;

        return ChatResponse.builder()
                .answer(answer)
                .sources(sources)
                .tokenUsage(0) // TODO: Extract from AI response
                .responseTime(responseTime)
                .hasReliableSource(!retrievalResults.isEmpty())
                .build();
    }

    private void enrichRetrievalResults(List<RetrievalResult> results) {
        for (RetrievalResult result : results) {
            if (result.getChunkText() == null) {
                List<KnowledgeChunk> chunks = chunkRepository.findByUserIdAndDocumentId(
                        SecurityUtils.getCurrentUserId(),
                        result.getDocumentId()
                );

                for (KnowledgeChunk chunk : chunks) {
                    if (chunk.getChunkIndex().equals(result.getChunkIndex())) {
                        result.setChunkText(chunk.getChunkText());
                        result.setDocumentName(chunk.getDocumentName());
                        result.setCategory(chunk.getCategory());
                        result.setSourceType(chunk.getSourceType());
                        result.setSourceUrl(chunk.getSourceUrl());
                        break;
                    }
                }
            }
        }
    }

    private String buildContext(List<RetrievalResult> results) {
        if (results.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder("以下是从用户知识库检索到的相关信息：\n\n");

        int index = 1;
        for (RetrievalResult result : results) {
            if (result.getChunkText() != null) {
                context.append(String.format("[文档%d: %s]\n", index, result.getDocumentName()));
                context.append(result.getChunkText());
                context.append("\n\n");
                index++;
            }
        }

        return context.toString();
    }

    private String buildSystemPrompt(UserProfileResponse profile, String context, boolean hasContext) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("你是一个专业的健身AI助手，名叫GymMind。你的任务是根据用户的个人信息和知识库，提供个性化的健身建议。\n\n");

        // Add user profile context
        if (profile != null) {
            prompt.append("## 用户画像\n");
            if (profile.getHeight() != null && profile.getWeight() != null) {
                prompt.append(String.format("- 身高: %.1f cm, 体重: %.1f kg\n", profile.getHeight(), profile.getWeight()));
            }
            if (profile.getFitnessGoal() != null) {
                prompt.append(String.format("- 健身目标: %s\n", profile.getFitnessGoal()));
            }
            if (profile.getExperienceLevel() != null) {
                prompt.append(String.format("- 经验水平: %s\n", profile.getExperienceLevel()));
            }
            if (profile.getWeeklyWorkoutDays() != null) {
                prompt.append(String.format("- 每周训练天数: %d\n", profile.getWeeklyWorkoutDays()));
            }
            prompt.append("\n");
        }

        // Add knowledge context
        if (hasContext) {
            prompt.append("## 知识库信息\n");
            prompt.append(context);
            prompt.append("\n");
            prompt.append("## 回答要求\n");
            prompt.append("1. 优先基于上述知识库信息回答问题\n");
            prompt.append("2. 结合用户的个人情况提供个性化建议\n");
            prompt.append("3. 如果知识库信息不足，可以结合你的专业知识补充，但要明确说明哪些是来自知识库，哪些是通用建议\n");
            prompt.append("4. 回答要专业、准确、实用\n");
        } else {
            prompt.append("## 回答要求\n");
            prompt.append("1. 用户的知识库中没有找到相关信息\n");
            prompt.append("2. 请基于你的专业知识提供通用的健身建议\n");
            prompt.append("3. 建议用户上传相关的健身资料以获得更个性化的建议\n");
            prompt.append("4. 回答要专业、准确、实用\n");
        }

        return prompt.toString();
    }

    private String generateAnswer(String systemPrompt, String userMessage, List<ChatMessage> history) {
        try {
            OpenAiApi openAiApi = new OpenAiApi(baseUrl, apiKey);
            ChatModel chatModel = new OpenAiChatModel(openAiApi);

            List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(systemPrompt));

            // Add history
            if (history != null && !history.isEmpty()) {
                for (ChatMessage msg : history) {
                    if ("user".equals(msg.getRole())) {
                        messages.add(new UserMessage(msg.getContent()));
                    } else {
                        messages.add(new org.springframework.ai.chat.messages.AssistantMessage(msg.getContent()));
                    }
                }
            }

            // Add current message
            messages.add(new UserMessage(userMessage));

            Prompt prompt = new Prompt(messages);
            String response = chatModel.call(prompt).getResult().getOutput().getContent();

            return response;

        } catch (Exception e) {
            log.error("Failed to generate answer", e);
            return "抱歉，AI服务暂时不可用，请稍后再试。";
        }
    }

    private List<SourceReference> buildSourceReferences(List<RetrievalResult> results) {
        return results.stream()
                .filter(r -> r.getChunkText() != null)
                .map(r -> SourceReference.builder()
                        .documentId(r.getDocumentId())
                        .documentName(r.getDocumentName())
                        .chunkIndex(r.getChunkIndex())
                        .excerpt(truncateText(r.getChunkText(), 200))
                        .category(r.getCategory())
                        .sourceType(r.getSourceType())
                        .sourceUrl(r.getSourceUrl())
                        .build())
                .collect(Collectors.toList());
    }

    private String truncateText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
