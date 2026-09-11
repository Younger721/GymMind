package com.gymmind.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymmind.dto.plan.GeneratePlanRequest;
import com.gymmind.dto.plan.WorkoutPlanResponse;
import com.gymmind.dto.profile.UserProfileResponse;
import com.gymmind.entity.WorkoutPlan;
import com.gymmind.entity.WorkoutPlanDay;
import com.gymmind.repository.WorkoutPlanDayRepository;
import com.gymmind.repository.WorkoutPlanRepository;
import com.gymmind.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkoutPlanGeneratorService {

    private final WorkoutPlanRepository planRepository;
    private final WorkoutPlanDayRepository planDayRepository;
    private final UserProfileService userProfileService;
    private final ObjectMapper objectMapper;

    @Value("${ai.openai.api-key}")
    private String apiKey;

    @Value("${ai.openai.base-url}")
    private String baseUrl;

    @Transactional
    public WorkoutPlanResponse generatePlan(GeneratePlanRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        log.info("Generating workout plan for userId={}, goal={}, weeks={}",
                userId, request.getGoal(), request.getDurationWeeks());

        // Get user profile
        UserProfileResponse profile = null;
        try {
            profile = userProfileService.getProfile();
        } catch (Exception e) {
            log.warn("Could not load user profile: {}", e.getMessage());
        }

        // Build AI prompt
        String aiPrompt = buildPlanGenerationPrompt(request, profile);

        // Generate plan with AI
        String aiResponse = callAIForPlanGeneration(aiPrompt);

        // Parse AI response to structured plan
        Map<String, Object> planData = parsePlanFromAI(aiResponse, request);

        // Save to database
        WorkoutPlan plan = savePlan(userId, request, planData);

        // Convert to response
        return convertToResponse(plan);
    }

    private String buildPlanGenerationPrompt(GeneratePlanRequest request, UserProfileResponse profile) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("你是一个专业的健身教练，请为用户生成一个个性化的训练计划。\n\n");

        prompt.append("## 用户信息\n");
        if (profile != null) {
            if (profile.getHeight() != null && profile.getWeight() != null) {
                prompt.append(String.format("- 身高: %.1f cm, 体重: %.1f kg\n", profile.getHeight(), profile.getWeight()));
            }
            if (profile.getExperienceLevel() != null) {
                prompt.append(String.format("- 训练经验: %s\n", profile.getExperienceLevel()));
            }
        }

        prompt.append("\n## 计划要求\n");
        prompt.append(String.format("- 目标: %s\n", translateGoal(request.getGoal())));
        prompt.append(String.format("- 时长: %d 周\n", request.getDurationWeeks()));
        prompt.append(String.format("- 每周训练: %d 天\n", request.getWorkoutsPerWeek()));
        prompt.append(String.format("- 难度: %s\n", translateDifficulty(request.getDifficulty())));
        prompt.append(String.format("- 设备条件: %s\n", translateEquipment(request.getEquipment())));
        prompt.append(String.format("- 单次时长: %d 分钟\n", request.getSessionDuration()));

        if (request.getFocusAreas() != null && request.getFocusAreas().length > 0) {
            prompt.append(String.format("- 重点部位: %s\n", String.join(", ", request.getFocusAreas())));
        }

        if (request.getInjuries() != null && request.getInjuries().length > 0) {
            prompt.append(String.format("- 需要避免的动作(有伤): %s\n", String.join(", ", request.getInjuries())));
        }

        prompt.append("\n## 输出要求\n");
        prompt.append("请以 JSON 格式输出训练计划，结构如下：\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"planName\": \"计划名称\",\n");
        prompt.append("  \"description\": \"计划描述\",\n");
        prompt.append("  \"weeks\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"weekNumber\": 1,\n");
        prompt.append("      \"weekTitle\": \"第1周：适应期\",\n");
        prompt.append("      \"days\": [\n");
        prompt.append("        {\n");
        prompt.append("          \"dayNumber\": 1,\n");
        prompt.append("          \"dayName\": \"胸+三头\",\n");
        prompt.append("          \"isRestDay\": false,\n");
        prompt.append("          \"exercises\": [\n");
        prompt.append("            {\n");
        prompt.append("              \"name\": \"杠铃卧推\",\n");
        prompt.append("              \"category\": \"STRENGTH\",\n");
        prompt.append("              \"muscleGroup\": \"CHEST\",\n");
        prompt.append("              \"sets\": 4,\n");
        prompt.append("              \"reps\": 8,\n");
        prompt.append("              \"restTime\": \"90s\",\n");
        prompt.append("              \"notes\": \"控制下放速度，顶峰收缩\"\n");
        prompt.append("            }\n");
        prompt.append("          ],\n");
        prompt.append("          \"notes\": \"训练前充分热身\"\n");
        prompt.append("        }\n");
        prompt.append("      ]\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n");
        prompt.append("```\n\n");

        prompt.append("## 注意事项\n");
        prompt.append("1. 计划要有渐进性，难度逐周递增\n");
        prompt.append("2. 每周要有1-2天休息日\n");
        prompt.append("3. 动作选择要符合目标和设备条件\n");
        prompt.append("4. 组数、次数要合理，符合训练水平\n");
        prompt.append("5. 要有训练提示和注意事项\n");
        prompt.append("6. 只输出 JSON，不要其他文字说明\n");

        return prompt.toString();
    }

    private String callAIForPlanGeneration(String prompt) {
        try {
            OpenAiApi openAiApi = new OpenAiApi(baseUrl, apiKey);
            ChatModel chatModel = new OpenAiChatModel(openAiApi);

            List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
            messages.add(new SystemMessage("你是一个专业的健身教练AI，擅长制定个性化训练计划。"));
            messages.add(new UserMessage(prompt));

            Prompt aiPrompt = new Prompt(messages);
            String response = chatModel.call(aiPrompt).getResult().getOutput().getContent();

            // Extract JSON from markdown code block if present
            if (response.contains("```json")) {
                int start = response.indexOf("```json") + 7;
                int end = response.lastIndexOf("```");
                response = response.substring(start, end).trim();
            } else if (response.contains("```")) {
                int start = response.indexOf("```") + 3;
                int end = response.lastIndexOf("```");
                response = response.substring(start, end).trim();
            }

            return response;

        } catch (Exception e) {
            log.error("Failed to generate plan with AI", e);
            // Fallback to template-based plan
            return generateTemplatePlan();
        }
    }

    private Map<String, Object> parsePlanFromAI(String aiResponse, GeneratePlanRequest request) {
        try {
            return objectMapper.readValue(aiResponse, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("Failed to parse AI response", e);
            // Return a basic template
            return createBasicPlanTemplate(request);
        }
    }

    private WorkoutPlan savePlan(Long userId, GeneratePlanRequest request, Map<String, Object> planData) {
        try {
            // Save main plan
            WorkoutPlan plan = WorkoutPlan.builder()
                    .userId(userId)
                    .planName((String) planData.get("planName"))
                    .description((String) planData.get("description"))
                    .goal(request.getGoal())
                    .difficulty(request.getDifficulty())
                    .durationWeeks(request.getDurationWeeks())
                    .workoutsPerWeek(request.getWorkoutsPerWeek())
                    .startDate(LocalDate.now())
                    .endDate(LocalDate.now().plusWeeks(request.getDurationWeeks()))
                    .planContent(objectMapper.writeValueAsString(planData))
                    .status("ACTIVE")
                    .isAiGenerated(true)
                    .build();

            plan = planRepository.save(plan);

            // Save plan days
            List<Map<String, Object>> weeks = (List<Map<String, Object>>) planData.get("weeks");
            if (weeks != null) {
                for (Map<String, Object> week : weeks) {
                    Integer weekNumber = (Integer) week.get("weekNumber");
                    List<Map<String, Object>> days = (List<Map<String, Object>>) week.get("days");

                    if (days != null) {
                        for (Map<String, Object> day : days) {
                            WorkoutPlanDay planDay = WorkoutPlanDay.builder()
                                    .planId(plan.getId())
                                    .weekNumber(weekNumber)
                                    .dayNumber((Integer) day.get("dayNumber"))
                                    .dayName((String) day.get("dayName"))
                                    .exercises(objectMapper.writeValueAsString(day.get("exercises")))
                                    .notes((String) day.get("notes"))
                                    .isRestDay((Boolean) day.getOrDefault("isRestDay", false))
                                    .isCompleted(false)
                                    .build();

                            planDayRepository.save(planDay);
                        }
                    }
                }
            }

            return plan;

        } catch (Exception e) {
            log.error("Failed to save plan", e);
            throw new RuntimeException("Failed to save workout plan");
        }
    }

    public WorkoutPlanResponse convertToResponse(WorkoutPlan plan) {
        try {
            Map<String, Object> planData = objectMapper.readValue(
                    plan.getPlanContent(),
                    new TypeReference<Map<String, Object>>() {}
            );

            List<WorkoutPlanResponse.WeekPlan> weeks = new ArrayList<>();
            List<Map<String, Object>> weeksData = (List<Map<String, Object>>) planData.get("weeks");

            if (weeksData != null) {
                for (Map<String, Object> weekData : weeksData) {
                    List<WorkoutPlanResponse.DayPlan> days = new ArrayList<>();
                    List<Map<String, Object>> daysData = (List<Map<String, Object>>) weekData.get("days");

                    if (daysData != null) {
                        for (Map<String, Object> dayData : daysData) {
                            List<WorkoutPlanResponse.Exercise> exercises = new ArrayList<>();
                            List<Map<String, Object>> exercisesData = (List<Map<String, Object>>) dayData.get("exercises");

                            if (exercisesData != null) {
                                exercises = exercisesData.stream()
                                        .map(ex -> WorkoutPlanResponse.Exercise.builder()
                                                .name((String) ex.get("name"))
                                                .category((String) ex.get("category"))
                                                .muscleGroup((String) ex.get("muscleGroup"))
                                                .sets((Integer) ex.get("sets"))
                                                .reps((Integer) ex.get("reps"))
                                                .restTime((String) ex.get("restTime"))
                                                .notes((String) ex.get("notes"))
                                                .build())
                                        .collect(Collectors.toList());
                            }

                            days.add(WorkoutPlanResponse.DayPlan.builder()
                                    .dayNumber((Integer) dayData.get("dayNumber"))
                                    .dayName((String) dayData.get("dayName"))
                                    .isRestDay((Boolean) dayData.getOrDefault("isRestDay", false))
                                    .isCompleted(false)
                                    .exercises(exercises)
                                    .notes((String) dayData.get("notes"))
                                    .build());
                        }
                    }

                    weeks.add(WorkoutPlanResponse.WeekPlan.builder()
                            .weekNumber((Integer) weekData.get("weekNumber"))
                            .weekTitle((String) weekData.get("weekTitle"))
                            .days(days)
                            .build());
                }
            }

            // Calculate stats
            int totalWorkouts = weeks.stream()
                    .flatMap(w -> w.getDays().stream())
                    .filter(d -> !d.getIsRestDay())
                    .collect(Collectors.toList())
                    .size();

            WorkoutPlanResponse.PlanStats stats = WorkoutPlanResponse.PlanStats.builder()
                    .totalWorkouts(totalWorkouts)
                    .completedWorkouts(0)
                    .remainingWorkouts(totalWorkouts)
                    .completionRate(0.0)
                    .currentWeek(1)
                    .build();

            return WorkoutPlanResponse.builder()
                    .id(plan.getId())
                    .planName(plan.getPlanName())
                    .description(plan.getDescription())
                    .goal(plan.getGoal())
                    .difficulty(plan.getDifficulty())
                    .durationWeeks(plan.getDurationWeeks())
                    .workoutsPerWeek(plan.getWorkoutsPerWeek())
                    .startDate(plan.getStartDate())
                    .endDate(plan.getEndDate())
                    .status(plan.getStatus())
                    .isAiGenerated(plan.getIsAiGenerated())
                    .createdAt(plan.getCreatedAt())
                    .weeks(weeks)
                    .stats(stats)
                    .build();

        } catch (Exception e) {
            log.error("Failed to convert plan to response", e);
            throw new RuntimeException("Failed to convert plan");
        }
    }

    public List<WorkoutPlanResponse> getUserPlans() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<WorkoutPlan> plans = planRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return plans.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public WorkoutPlanResponse getPlanById(Long planId) {
        Long userId = SecurityUtils.getCurrentUserId();
        WorkoutPlan plan = planRepository.findByIdAndUserId(planId, userId)
                .orElseThrow(() -> new RuntimeException("Plan not found"));

        return convertToResponse(plan);
    }

    private String translateGoal(String goal) {
        return switch (goal) {
            case "MUSCLE_GAIN" -> "增肌";
            case "WEIGHT_LOSS" -> "减脂";
            case "STRENGTH" -> "力量提升";
            case "ENDURANCE" -> "耐力提升";
            default -> "综合健身";
        };
    }

    private String translateDifficulty(String difficulty) {
        return switch (difficulty) {
            case "BEGINNER" -> "初级";
            case "INTERMEDIATE" -> "中级";
            case "ADVANCED" -> "高级";
            default -> "中级";
        };
    }

    private String translateEquipment(String equipment) {
        return switch (equipment) {
            case "GYM" -> "健身房全套器械";
            case "HOME" -> "家用基础器械";
            case "MINIMAL" -> "徒手/最简器械";
            default -> "健身房";
        };
    }

    private String generateTemplatePlan() {
        return """
        {
          "planName": "4周基础增肌计划",
          "description": "适合健身初学者的渐进式增肌训练",
          "weeks": [
            {
              "weekNumber": 1,
              "weekTitle": "第1周：适应期",
              "days": [
                {
                  "dayNumber": 1,
                  "dayName": "胸部+三头",
                  "isRestDay": false,
                  "exercises": [
                    {
                      "name": "杠铃卧推",
                      "category": "STRENGTH",
                      "muscleGroup": "CHEST",
                      "sets": 3,
                      "reps": 10,
                      "restTime": "90s",
                      "notes": "控制动作，感受胸部发力"
                    },
                    {
                      "name": "哑铃飞鸟",
                      "category": "STRENGTH",
                      "muscleGroup": "CHEST",
                      "sets": 3,
                      "reps": 12,
                      "restTime": "60s",
                      "notes": "顶峰收缩，充分拉伸"
                    }
                  ],
                  "notes": "第一周以适应训练为主，不要追求大重量"
                }
              ]
            }
          ]
        }
        """;
    }

    private Map<String, Object> createBasicPlanTemplate(GeneratePlanRequest request) {
        // Return a minimal valid structure
        return Map.of(
                "planName", "AI生成失败，使用默认计划",
                "description", "请重试生成或手动创建计划",
                "weeks", List.of()
        );
    }
}
