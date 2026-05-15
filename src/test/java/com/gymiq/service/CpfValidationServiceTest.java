package com.gymiq.service;

import com.gymiq.exception.BusinessException;
import com.gymiq.integration.BrasilApiCpfClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpfValidationServiceTest {

    @Mock
    private BrasilApiCpfClient brasilApiCpfClient;

    @InjectMocks
    private CpfValidationService cpfValidationService;

    @Test
    void validateShouldAcceptCpfWithValidCheckDigitsAndExistingDocument() {
        when(brasilApiCpfClient.exists("12345678909")).thenReturn(true);

        cpfValidationService.validate("123.456.789-09");

        verify(brasilApiCpfClient).exists("12345678909");
    }

    @Test
    void validateShouldRejectCpfWithRepeatedDigitsBeforeCallingExternalApi() {
        assertThatThrownBy(() -> cpfValidationService.validate("111.111.111-11"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CPF");

        verify(brasilApiCpfClient, never()).exists("11111111111");
    }

    @Test
    void validateShouldRejectCpfNotFoundByExternalApi() {
        when(brasilApiCpfClient.exists("12345678909")).thenReturn(false);

        assertThatThrownBy(() -> cpfValidationService.validate("123.456.789-09"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não encontrado");
    }
}
