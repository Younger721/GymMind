package com.gymmind.ai.chat.application;
import java.util.*;
public record AiRoleProfile(String roleCode,String assistantName,String systemPrompt,List<String> quickPrompts,List<String> enabledPlugins,int maxHistoryTurns) {}
