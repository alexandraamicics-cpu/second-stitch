package com.secondstitch.common;

import com.secondstitch.auth.EmailNotVerifiedException;
import com.secondstitch.auth.InvalidLinkException;
import com.secondstitch.auth.LoginFailedException;
import com.secondstitch.auth.TooManyAttemptsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns exceptions into clean JSON error responses, so the frontend
 * always gets the same shape: { "message": "...", "fields": { ... } }.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(NotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> invalid(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        return error(HttpStatus.BAD_REQUEST, "Some fields need fixing", fields);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> conflict(ConflictException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(LoginFailedException.class)
    public ResponseEntity<Map<String, Object>> loginFailed(LoginFailedException ex) {
        return error(HttpStatus.UNAUTHORIZED, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<Map<String, Object>> notVerified(EmailNotVerifiedException ex) {
        ResponseEntity<Map<String, Object>> response = error(HttpStatus.FORBIDDEN, ex.getMessage(), Map.of());
        // The frontend checks this code to show a "Send the link again" button
        response.getBody().put("code", "EMAIL_NOT_VERIFIED");
        return response;
    }

    @ExceptionHandler(InvalidLinkException.class)
    public ResponseEntity<Map<String, Object>> invalidLink(InvalidLinkException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(TooManyAttemptsException.class)
    public ResponseEntity<Map<String, Object>> tooMany(TooManyAttemptsException ex) {
        return error(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> tooLarge(MaxUploadSizeExceededException ex) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "Photos must be 5 MB or smaller", Map.of());
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message, Map<String, String> fields) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        body.put("fields", fields);
        return ResponseEntity.status(status).body(body);
    }
}
