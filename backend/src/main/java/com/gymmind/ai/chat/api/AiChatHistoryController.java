package com.gymmind.ai.chat.api;
import com.gymmind.ai.chat.application.*; import com.gymmind.ai.chat.domain.model.*; import com.gymmind.shared.api.ApiResponse; import com.gymmind.shared.security.*; import jakarta.validation.Valid; import jakarta.validation.constraints.NotBlank; import org.springframework.web.bind.annotation.*; import java.time.Instant; import java.util.*;
@RestController @RequestMapping("/api/v1/ai/chat/sessions") public class AiChatHistoryController{
 private final AiChatHistoryService service; private final CurrentActorProvider actors; public AiChatHistoryController(AiChatHistoryService s,CurrentActorProvider a){service=s;actors=a;}
 @GetMapping public ApiResponse<List<SessionView>> list(){return ApiResponse.success(service.list(actors.requireCurrent()).stream().map(SessionView::of).toList());}
 @PostMapping public ApiResponse<SessionView> create(@Valid @RequestBody SessionRequest r){return ApiResponse.success(SessionView.of(service.ensureSession(actors.requireCurrent(),r.sessionKey(),r.title())));}
 @GetMapping("/{sessionKey}/messages") public ApiResponse<List<MessageView>> messages(@PathVariable String sessionKey){return ApiResponse.success(service.messages(actors.requireCurrent(),sessionKey).stream().map(MessageView::of).toList());}
 @DeleteMapping("/{sessionKey}") public ApiResponse<Void> archive(@PathVariable String sessionKey){service.archive(actors.requireCurrent(),sessionKey);return ApiResponse.success(null);}
 public record SessionRequest(@NotBlank String sessionKey,String title){} public record SessionView(String sessionKey,String roleContext,String title,Instant updatedAt){static SessionView of(AiChatSession s){return new SessionView(s.getSessionKey(),s.getRoleContext(),s.getTitle(),s.getUpdatedAt());}}
 public record MessageView(String role,String content,long sequence,Instant createdAt){static MessageView of(AiChatMessage m){return new MessageView(m.getRole().name().toLowerCase(),m.getContent(),m.getSequence(),m.getCreatedAt());}}
}
