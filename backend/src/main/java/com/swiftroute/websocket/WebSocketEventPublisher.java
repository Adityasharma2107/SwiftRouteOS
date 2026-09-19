package com.swiftroute.websocket;

import com.swiftroute.dto.websocket.WebSocketMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;

@Component
public class WebSocketEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventPublisher.class);

    private final SimpMessageSendingOperations messagingTemplate;

    public WebSocketEventPublisher(SimpMessageSendingOperations messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishJobEvent(String eventType, Object payload) {
        try {
            WebSocketMessage<Object> message = new WebSocketMessage<>(eventType, payload);
            messagingTemplate.convertAndSend("/topic/jobs", message);
            log.debug("Published WebSocket event to /topic/jobs: type={}", eventType);
        } catch (Exception ex) {
            log.error("Failed to publish WebSocket event to /topic/jobs: {}", ex.getMessage());
        }
    }

    public void publishSlaAlert(String eventType, Object payload) {
        try {
            WebSocketMessage<Object> message = new WebSocketMessage<>(eventType, payload);
            messagingTemplate.convertAndSend("/topic/sla-alerts", message);
            log.info("Published SLA alert to /topic/sla-alerts: type={}", eventType);
        } catch (Exception ex) {
            log.error("Failed to publish WebSocket SLA alert: {}", ex.getMessage());
        }
    }

    public void publishInventoryUpdate(String eventType, Object payload) {
        try {
            WebSocketMessage<Object> message = new WebSocketMessage<>(eventType, payload);
            messagingTemplate.convertAndSend("/topic/inventory", message);
            log.debug("Published WebSocket inventory update to /topic/inventory: type={}", eventType);
        } catch (Exception ex) {
            log.error("Failed to publish WebSocket inventory event: {}", ex.getMessage());
        }
    }
}
