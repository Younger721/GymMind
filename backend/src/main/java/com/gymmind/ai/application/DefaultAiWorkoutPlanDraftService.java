package com.gymmind.ai.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.workout.application.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class DefaultAiWorkoutPlanDraftService implements AiWorkoutPlanDraftService {
    private final ChatModelGateway model;
    private final ObjectMapper mapper;
    private final WorkoutPlanValidator validator = new WorkoutPlanValidator();

    public DefaultAiWorkoutPlanDraftService(ChatModelGateway model, ObjectMapper mapper) {
        this.model = model;
        this.mapper = mapper;
    }

    @Override
    public CreateWorkoutPlanCommand generate(CurrentActor actor, Long memberId, String request) {
        if (actor == null || actor.tenantId() == null || !actor.hasPermission("ai:plan") || memberId == null || request == null || request.isBlank()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        final String response;
        try {
            response = model.complete("请仅输出结构化训练计划JSON，不要输出解释：" + request, List.of());
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE);
        }
        if (response == null || response.isBlank()) throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        try {
            JsonNode root = mapper.readTree(stripFences(response));
            List<WorkoutPlanItemCommand> items = new ArrayList<>();
            for (JsonNode item : root.path("items")) {
                items.add(new WorkoutPlanItemCommand(item.path("exerciseId").asLong(), item.path("dayOfWeek").asInt(),
                        item.path("sets").asInt(), item.path("reps").asInt(), item.path("restSeconds").asInt(),
                        item.path("weight").asDouble(), item.path("notes").asText("")));
            }
            var command = new CreateWorkoutPlanCommand(actor.tenantId(), memberId, null,
                    root.path("name").asText(), root.path("goal").asText(),
                    LocalDate.parse(root.path("startDate").asText()), LocalDate.parse(root.path("endDate").asText()),
                    root.path("description").asText(), items);
            validator.validate(command);
            return command;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }

    private String stripFences(String response) {
        String value = response.trim();
        if (value.startsWith("```")) {
            int firstNewline = value.indexOf('\n');
            int lastFence = value.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) value = value.substring(firstNewline + 1, lastFence).trim();
        }
        return value;
    }
}
