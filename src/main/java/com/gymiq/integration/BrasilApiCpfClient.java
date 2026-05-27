package com.gymiq.integration;

import com.gymiq.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class BrasilApiCpfClient {

    private final RestTemplate restTemplate;

    @Value("${gymiq.api.brasilapi.url}")
    private String baseUrl;

    public boolean exists(String cpf) {
        String normalizedCpf = normalizeCpf(cpf);
        String url = baseUrl + "/" + normalizedCpf;

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException ex) {
            log.warn("Nao foi possivel consultar BrasilAPI para CPF {}. Mantendo validacao local.", normalizedCpf, ex);
            return true;
        }
    }

    private String normalizeCpf(String cpf) {
        String digits = cpf == null ? "" : cpf.replaceAll("\\D", "");
        if (digits.length() != 11) {
            throw new BusinessException("CPF deve conter 11 digitos");
        }
        return digits;
    }
}
