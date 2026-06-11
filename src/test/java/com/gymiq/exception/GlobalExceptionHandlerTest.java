package com.gymiq.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private WebRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/test");
    }

    @Test
    void handleBusinessShouldReturnUnprocessableEntity() {
        var response = handler.handleBusiness(new BusinessException("Regra inválida"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertBody(response.getBody(), 422, GlobalExceptionHandler.REGRA_NEGOCIO_VIOLADA, "Regra inválida");
    }

    @Test
    void handleNotFoundShouldReturnNotFound() {
        var response = handler.handleNotFound(new ResourceNotFoundException("Registro não encontrado"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertBody(response.getBody(), 404, GlobalExceptionHandler.RECURSO_NAO_ENCONTRADO, "Registro não encontrado");
    }

    @Test
    void handleTypeMismatchShouldReturnInvalidParameter() {
        var exception = new MethodArgumentTypeMismatchException(
                "abc",
                Integer.class,
                "id",
                null,
                new NumberFormatException("invalid"));

        var response = handler.handleTypeMismatch(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertBody(response.getBody(), 400, GlobalExceptionHandler.PARAMETRO_INVALIDO, "Parâmetro inválido: id deve ser do tipo Integer");
    }

    @Test
    void handleInvalidParameterShouldReturnBadRequest() {
        var response = handler.handleInvalidParameter(new InvalidParameterException("Filtro inválido"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertBody(response.getBody(), 400, GlobalExceptionHandler.PARAMETRO_INVALIDO, "Filtro inválido");
    }

    @Test
    void handleBadCredentialsShouldReturnUnauthorized() {
        var response = handler.handleBadCredentials(new BadCredentialsException("bad credentials"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertBody(response.getBody(), 401, GlobalExceptionHandler.CREDENCIAIS_INVALIDAS, "E-mail ou senha incorretos");
    }

    @Test
    void handleAccessDeniedShouldReturnForbidden() {
        var response = handler.handleAccessDenied(new AccessDeniedException("denied"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertBody(response.getBody(), 403, GlobalExceptionHandler.ACESSO_NEGADO, "Seu perfil não tem permissão para esta operação");
    }

    @Test
    void handleGenericShouldReturnInternalServerError() {
        var response = handler.handleGeneric(new RuntimeException("Erro técnico"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertBody(response.getBody(), 500, GlobalExceptionHandler.ERRO_INTERNO, "Ocorreu um erro inesperado. Tente novamente.");
    }

    private void assertBody(Map<String, Object> body, int statusCode, String error, String message) {
        assertThat(body)
                .containsEntry("statusCode", statusCode)
                .containsEntry("error", error)
                .containsEntry("message", message)
                .containsEntry("path", "/api/test")
                .containsKey("timestamp");
    }
}
