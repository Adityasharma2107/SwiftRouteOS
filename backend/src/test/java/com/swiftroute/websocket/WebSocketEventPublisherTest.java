package com.swiftroute.websocket;

import com.swiftroute.dto.websocket.WebSocketMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WebSocketEventPublisherTest {

    private TestSimpMessageSendingOperations sendingOperations;
    private WebSocketEventPublisher publisher;

    @BeforeEach
    void setUp() {
        sendingOperations = new TestSimpMessageSendingOperations();
        publisher = new WebSocketEventPublisher(sendingOperations);
    }

    @Test
    @DisplayName("Publisher sends job events to /topic/jobs destination")
    void testPublishJobEvent() {
        publisher.publishJobEvent("JOB_STATUS_CHANGED", Map.of("jobId", 1L, "status", "ASSIGNED"));

        assertEquals(1, sendingOperations.sentDestinations.size());
        assertEquals("/topic/jobs", sendingOperations.sentDestinations.get(0));
        assertTrue(sendingOperations.sentPayloads.get(0) instanceof WebSocketMessage<?>);
        WebSocketMessage<?> msg = (WebSocketMessage<?>) sendingOperations.sentPayloads.get(0);
        assertEquals("JOB_STATUS_CHANGED", msg.getType());
    }

    @Test
    @DisplayName("Publisher sends SLA alerts to /topic/sla-alerts destination")
    void testPublishSlaAlert() {
        publisher.publishSlaAlert("SLA_BREACHED", Map.of("jobId", 2L, "stage", "BREACHED"));

        assertEquals(1, sendingOperations.sentDestinations.size());
        assertEquals("/topic/sla-alerts", sendingOperations.sentDestinations.get(0));
        WebSocketMessage<?> msg = (WebSocketMessage<?>) sendingOperations.sentPayloads.get(0);
        assertEquals("SLA_BREACHED", msg.getType());
    }

    @Test
    @DisplayName("Publisher sends inventory updates to /topic/inventory destination")
    void testPublishInventoryUpdate() {
        publisher.publishInventoryUpdate("STOCK_RESERVED", Map.of("itemId", 3L, "qty", 2));

        assertEquals(1, sendingOperations.sentDestinations.size());
        assertEquals("/topic/inventory", sendingOperations.sentDestinations.get(0));
        WebSocketMessage<?> msg = (WebSocketMessage<?>) sendingOperations.sentPayloads.get(0);
        assertEquals("STOCK_RESERVED", msg.getType());
    }

    @Test
    @DisplayName("Publisher catches messaging exceptions gracefully without rethrowing")
    void testPublisherErrorResilience() {
        sendingOperations.throwException = true;
        assertDoesNotThrow(() -> publisher.publishJobEvent("FAIL_EVENT", Map.of()));
    }

    private static class TestSimpMessageSendingOperations implements SimpMessageSendingOperations {
        final List<String> sentDestinations = new ArrayList<>();
        final List<Object> sentPayloads = new ArrayList<>();
        boolean throwException = false;

        @Override
        public void convertAndSend(String destination, Object payload) throws MessagingException {
            if (throwException) {
                throw new MessagingException("Simulated broker transport failure");
            }
            sentDestinations.add(destination);
            sentPayloads.add(payload);
        }

        @Override
        public void convertAndSend(String destination, Object payload, Map<String, Object> headers) throws MessagingException {
            convertAndSend(destination, payload);
        }

        @Override
        public void convertAndSend(String destination, Object payload, MessagePostProcessor postProcessor) throws MessagingException {
            convertAndSend(destination, payload);
        }

        @Override
        public void convertAndSend(String destination, Object payload, Map<String, Object> headers, MessagePostProcessor postProcessor) throws MessagingException {
            convertAndSend(destination, payload);
        }

        @Override
        public void convertAndSend(Object payload) throws MessagingException {}
        @Override
        public void convertAndSend(Object payload, MessagePostProcessor postProcessor) throws MessagingException {}
        @Override
        public void convertAndSendToUser(String user, String destination, Object payload) throws MessagingException {}
        @Override
        public void convertAndSendToUser(String user, String destination, Object payload, Map<String, Object> headers) throws MessagingException {}
        @Override
        public void convertAndSendToUser(String user, String destination, Object payload, MessagePostProcessor postProcessor) throws MessagingException {}
        @Override
        public void convertAndSendToUser(String user, String destination, Object payload, Map<String, Object> headers, MessagePostProcessor postProcessor) throws MessagingException {}
        @Override
        public void send(Message<?> message) throws MessagingException {}
        @Override
        public void send(String destination, Message<?> message) throws MessagingException {}
    }
}
