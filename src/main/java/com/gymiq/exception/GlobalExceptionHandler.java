package com.gymiq.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.*;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex, WebRequest request) {

        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String campo = error instanceof FieldError fe ? fe.getField() : error.getObjectName();
            campos.put(campo, error.getDefaultMessage());
        });

        return ResponseEntity.badRequest().body(erroBody(
                400, "Erro de validação", "Corrija os campos indicados",
                request.getDescription(false), campos));
    }


    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(
            BusinessException ex, WebRequest request) {
        return ResponseEntity.unprocessableEntity().body(erroBody(
                422, "Regra de negócio violada", ex.getMessage(),
                request.getDescription(false), null));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            ResourceNotFoundException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erroBody(
                404, "Recurso não encontrado", ex.getMessage(),
                request.getDescription(false), null));
    }


    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(
            BadCredentialsException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(erroBody(
                401, "Credenciais inválidas", "E-mail ou senha incorretos",
                request.getDescription(false), null));
    }


    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(erroBody(
                403, "Acesso negado", "Seu perfil não tem permissão para esta operação",
                request.getDescription(false), null));
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(
            Exception ex, WebRequest request) {
        log.error("Erro interno não tratado", ex);
        return ResponseEntity.internalServerError().body(erroBody(
                500, "Erro interno", "Ocorreu um erro inesperado. Tente novamente.",
                request.getDescription(false), null));
    }


    private Map<String, Object> erroBody(int status, String erro, String mensagem,
                                          String path, Map<String, String> campos) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status);
        body.put("erro", erro);
        body.put("mensagem", mensagem);
        body.put("path", path.replace("uri=", ""));
        if (campos != null) body.put("campos", campos);
        return body;
    }
}
