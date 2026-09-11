package com.swiftroute.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private int status;
    private String errorCode;
    private String message;
    private String path;
    private Map<String, Object> details;
    private Instant timestamp;

    public ErrorResponse() {
    }

    public ErrorResponse(int status, String errorCode, String message, String path, Map<String, Object> details, Instant timestamp) {
        this.status = status;
        this.errorCode = errorCode;
        this.message = message;
        this.path = path;
        this.details = details;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int status;
        private String errorCode;
        private String message;
        private String path;
        private Map<String, Object> details;
        private Instant timestamp = Instant.now();

        public Builder status(int status) {
            this.status = status;
            return this;
        }

        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder details(Map<String, Object> details) {
            this.details = details;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public ErrorResponse build() {
            return new ErrorResponse(status, errorCode, message, path, details, timestamp);
        }
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
