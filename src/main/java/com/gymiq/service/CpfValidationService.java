package com.gymiq.service;

import com.gymiq.exception.BusinessException;
import com.gymiq.integration.BrasilApiCpfClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CpfValidationService {

    private final BrasilApiCpfClient brasilApiCpfClient;

    @Value("${gymiq.cpf.validation-mode:LOCAL}")
    private String validationMode;

    @PostConstruct
    void validateConfiguration() {
        resolveValidationMode();
    }

    public void validate(String cpf) {
        CpfValidationMode mode = resolveValidationMode();

        if (mode == CpfValidationMode.DISABLED) {
            return;
        }

        String normalizedCpf = normalizeCpf(cpf);

        if (!hasValidCheckDigits(normalizedCpf)) {
            throw new BusinessException("CPF invalido");
        }

        if (mode == CpfValidationMode.EXTERNAL && !brasilApiCpfClient.exists(normalizedCpf)) {
            throw new BusinessException("CPF nao encontrado / nao existente.");
        }
    }

    private CpfValidationMode resolveValidationMode() {
        try {
            return CpfValidationMode.valueOf(validationMode.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Modo de validacao de CPF invalido: " + validationMode);
        }
    }

    private boolean hasValidCheckDigits(String cpf) {
        if (cpf.chars().distinct().count() == 1) {
            return false;
        }

        int firstDigit = calculateDigit(cpf, 9);
        int secondDigit = calculateDigit(cpf, 10);

        return firstDigit == Character.getNumericValue(cpf.charAt(9))
                && secondDigit == Character.getNumericValue(cpf.charAt(10));
    }

    private int calculateDigit(String cpf, int length) {
        int sum = 0;
        for (int index = 0; index < length; index++) {
            sum += Character.getNumericValue(cpf.charAt(index)) * (length + 1 - index);
        }

        int result = 11 - (sum % 11);
        return result >= 10 ? 0 : result;
    }

    private String normalizeCpf(String cpf) {
        String digits = cpf == null ? "" : cpf.replaceAll("\\D", "");
        if (digits.length() != 11) {
            throw new BusinessException("CPF deve conter 11 digitos");
        }
        return digits;
    }

    private enum CpfValidationMode {
        LOCAL,
        EXTERNAL,
        DISABLED
    }
}
