package com.gymiq.service;

import com.gymiq.exception.BusinessException;
import com.gymiq.integration.BrasilApiCpfClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CpfValidationService {

    private final BrasilApiCpfClient brasilApiCpfClient;

    public void validate(String cpf) {
        String normalizedCpf = normalizeCpf(cpf);

        if (!hasValidCheckDigits(normalizedCpf)) {
            throw new BusinessException("CPF invalido");
        }

        if (!brasilApiCpfClient.exists(normalizedCpf)) {
            throw new BusinessException("CPF não encontrado / não existente.");
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
}
