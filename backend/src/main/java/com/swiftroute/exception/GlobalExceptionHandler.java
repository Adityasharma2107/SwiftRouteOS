package com.swiftroute.exception;

import com.swiftroute.common.ApiResponse;
import com.swiftroute.common.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(InsufficientInventoryException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientInventory(InsufficientInventoryException ex, HttpServletRequest request) {
        log.warn("Inventory conflict: {}", ex.getMessage());
        Map<String, Object> details = Map.of(
                "itemId", ex.getItemId(),
                "partNumber", ex.getPartNumber(),
                "requested", ex.getRequested(),
                "available", ex.getAvailable()
        );
        return buildErrorResponse(HttpStatus.CONFLICT, "INSUFFICIENT_INVENTORY", ex.getMessage(), request.getRequestURI(), details);
    }

    @ExceptionHandler(TechnicianConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleTechnicianConflict(TechnicianConflictException ex, HttpServletRequest request) {
        log.warn("Technician scheduling conflict: {}", ex.getMessage());
        Map<String, Object> details = new HashMap<>();
        details.put("technicianId", ex.getTechnicianId());
        if (ex.getConflictStart() != null) details.put("conflictStart", ex.getConflictStart());
        if (ex.getConflictEnd() != null) details.put("conflictEnd", ex.getConflictEnd());

        return buildErrorResponse(HttpStatus.CONFLICT, "TECHNICIAN_CONFLICT", ex.getMessage(), request.getRequestURI(), details);
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidStateTransition(InvalidStateTransitionException ex, HttpServletRequest request) {
        log.warn("Invalid state transition: {}", ex.getMessage());
        Map<String, Object> details = Map.of(
                "fromStatus", ex.getFromStatus(),
                "toStatus", ex.getToStatus()
        );
        return buildErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_STATE_TRANSITION", ex.getMessage(), request.getRequestURI(), details);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessRule(BusinessRuleException ex, HttpServletRequest request) {
        log.warn("Business rule violation: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "BUSINESS_RULE_VIOLATION", ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, Object> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", request.getRequestURI(), fieldErrors);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied: insufficient permissions", request.getRequestURI(), null);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Invalid username or password", request.getRequestURI(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled server error at {}", request.getRequestURI(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "An unexpected error occurred", request.getRequestURI(), null);
    }

    private ResponseEntity<ApiResponse<Void>> buildErrorResponse(
            HttpStatus status,
            String errorCode,
            String message,
            String path,
            Map<String, Object> details) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(status.value())
                .errorCode(errorCode)
                .message(message)
                .path(path)
                .details(details)
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(status).body(ApiResponse.fail(errorResponse));
    }
}
