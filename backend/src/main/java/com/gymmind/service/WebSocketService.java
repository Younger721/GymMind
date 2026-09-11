package com.gymmind.service;

import com.gymmind.dto.websocket.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendToUser(String userId, WebSocketMessage message) {
        String destination = "/queue/messages-" + userId;
        messagingTemplate.convertAndSend(destination, message);
        log.debug("Sent WebSocket message to user {}: type={}", userId, message.getType());
    }

    public void sendDocumentProgress(String userId, Long documentId, String status, Double progress) {
        WebSocketMessage message = WebSocketMessage.documentProgress(userId, documentId, status, progress);
        sendToUser(userId, message);
    }

    public void sendAiStream(String userId, String chunk, boolean isComplete) {
        WebSocketMessage message = WebSocketMessage.aiStream(userId, chunk, isComplete);
        sendToUser(userId, message);
    }

    public void sendNotification(String userId, String title, String message, String level) {
        WebSocketMessage notification = WebSocketMessage.notification(userId, title, message, level);
        sendToUser(userId, notification);
    }

    public void broadcastToAll(WebSocketMessage message) {
        messagingTemplate.convertAndSend("/topic/broadcast", message);
        log.debug("Broadcast WebSocket message: type={}", message.getType());
    }
}
