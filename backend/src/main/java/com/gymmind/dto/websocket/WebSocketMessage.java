package com.gymmind.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {

    private String type; // DOCUMENT_PROGRESS, AI_STREAM, NOTIFICATION

    private String userId;

    private Object payload;

    private LocalDateTime timestamp;

    public static WebSocketMessage documentProgress(String userId, Long documentId, String status, Double progress) {
        return WebSocketMessage.builder()
                .type("DOCUMENT_PROGRESS")
                .userId(userId)
                .payload(DocumentProgressPayload.builder()
                        .documentId(documentId)
                        .status(status)
                        .progress(progress)
                        .build())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static WebSocketMessage aiStream(String userId, String chunk, boolean isComplete) {
        return WebSocketMessage.builder()
                .type("AI_STREAM")
                .userId(userId)
                .payload(AiStreamPayload.builder()
                        .chunk(chunk)
                        .isComplete(isComplete)
                        .build())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static WebSocketMessage notification(String userId, String title, String message, String level) {
        return WebSocketMessage.builder()
                .type("NOTIFICATION")
                .userId(userId)
                .payload(NotificationPayload.builder()
                        .title(title)
                        .message(message)
                        .level(level)
                        .build())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentProgressPayload {
        private Long documentId;
        private String status;
        private Double progress;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiStreamPayload {
        private String chunk;
        private Boolean isComplete;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationPayload {
        private String title;
        private String message;
        private String level; // info, success, warning, error
    }
}
