package com.gymmind.ai.application; import com.gymmind.shared.security.CurrentActor; public interface AiChatService { AiChatView chat(CurrentActor actor,String sessionId,String question); }
