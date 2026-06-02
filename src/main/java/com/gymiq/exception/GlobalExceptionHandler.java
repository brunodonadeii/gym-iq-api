package com.gymiq.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex, WebRequest request) {

        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = error instanceof FieldError fieldError
                    ? fieldError.getField()
                    : error.getObjectName();
            fields.put(field, error.getDefaultMessage());
        });

        return ResponseEntity.badRequest().body(errorBody(
                400,
                "VALIDATION_ERROR",
                "Corrija os campos indicados",
                request.getDescription(false),
                fields));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(
            BusinessException ex, WebRequest request) {
        return ResponseEntity.unprocessableEntity().body(errorBody(
                422,
                "BUSINESS_RULE_VIOLATION",
                ex.getMessage(),
                request.getDescription(false),
                null));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            ResourceNotFoundException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(
                404,
                "RESOURCE_NOT_FOUND",
                ex.getMessage(),
                request.getDescription(false),
                null));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {

        String requiredType = ex.getRequiredType() != null
                ? ex.getRequiredType().getSimpleName()
                : "tipo esperado";

        String message = "Parametro invalido: " + ex.getName()
                + " deve ser do tipo " + requiredType;

        return ResponseEntity.badRequest().body(errorBody(
                400,
                "INVALID_PARAMETER",
                message,
                request.getDescription(false),
                null));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(
            BadCredentialsException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody(
                401,
                "INVALID_CREDENTIALS",
                "E-mail ou senha incorretos",
                request.getDescription(false),
                null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody(
                403,
                "ACCESS_DENIED",
                "Seu perfil nao tem permissao para esta operacao",
                request.getDescription(false),
                null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(
            Exception ex, WebRequest request) {
        log.error("Erro interno nao tratado", ex);
        return ResponseEntity.internalServerError().body(errorBody(
                500,
                "INTERNAL_ERROR",
                "Ocorreu um erro inesperado. Tente novamente.",
                request.getDescription(false),
                null));
    }

    private Map<String, Object> errorBody(
            int statusCode,
            String error,
            String message,
            String path,
            Map<String, String> fields) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("statusCode", statusCode);
        body.put("error", error);
        body.put("message", message);
        body.put("path", path.replace("uri=", ""));
        if (fields != null) {
            body.put("fields", fields);
        }
        return body;
    }
}
