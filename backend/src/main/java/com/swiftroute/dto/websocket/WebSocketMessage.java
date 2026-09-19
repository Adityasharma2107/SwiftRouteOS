package com.swiftroute.dto.websocket;

import java.time.Instant;

public class WebSocketMessage<T> {

    private String type;
    private Instant timestamp;
    private T payload;

    public WebSocketMessage() {
    }

    public WebSocketMessage(String type, T payload) {
        this.type = type;
        this.timestamp = Instant.now();
        this.payload = payload;
    }

    public WebSocketMessage(String type, Instant timestamp, T payload) {
        this.type = type;
        this.timestamp = timestamp;
        this.payload = payload;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }
}
