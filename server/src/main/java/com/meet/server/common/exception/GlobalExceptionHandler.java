package com.meet.server.common.exception;

import com.meet.server.common.api.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception
    ) {
        return validationResponse(exception.getBindingResult().getFieldErrors(),
                exception.getBindingResult().getGlobalErrors());
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleBindException(BindException exception) {
        return validationResponse(exception.getBindingResult().getFieldErrors(),
                exception.getBindingResult().getGlobalErrors());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolation(
            ConstraintViolationException exception
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getConstraintViolations().forEach(violation ->
                errors.put(violation.getPropertyPath().toString(), violation.getMessage()));
        return badRequest("Validation failed", errors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodValidation(
            HandlerMethodValidationException exception
    ) {
        return badRequest("Validation failed", null);
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthException(AuthException exception) {
        return response(exception.getStatus(), exception.getMessage());
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidToken(InvalidTokenException exception) {
        return response(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatusException(ResponseStatusException exception) {
        return response(exception.getStatusCode(), exception.getReason());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception exception) {
        return response(HttpStatus.BAD_REQUEST, "Malformed or incomplete request");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        log.error("Unhandled application exception", exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private ResponseEntity<ApiResponse<Map<String, String>>> validationResponse(
            Iterable<FieldError> fieldErrors,
            Iterable<ObjectError> globalErrors
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        fieldErrors.forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        globalErrors.forEach(error -> errors.put(error.getObjectName(), error.getDefaultMessage()));
        return badRequest("Validation failed", errors);
    }

    private <T> ResponseEntity<ApiResponse<T>> badRequest(String message, T data) {
        return response(HttpStatus.BAD_REQUEST, message, data);
    }

    private <T> ResponseEntity<ApiResponse<T>> response(HttpStatusCode status, String message) {
        return response(status, message, null);
    }

    private <T> ResponseEntity<ApiResponse<T>> response(HttpStatusCode status, String message, T data) {
        Optional<T> responseData = data == null ? Optional.empty() : Optional.of(data);
        return ResponseEntity.status(status).body(new ApiResponse<>(false, message, responseData));
    }
}
