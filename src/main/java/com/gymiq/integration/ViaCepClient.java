package com.gymiq.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gymiq.dto.response.AddressLookupResponse;
import com.gymiq.exception.BusinessException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class ViaCepClient {

    private final RestTemplate restTemplate;

    @Value("${gymiq.api.viacep.url}")
    private String baseUrl;

    public AddressLookupResponse lookup(String zipCode) {
        String normalizedZipCode = normalizeZipCode(zipCode);
        String url = baseUrl + "/" + normalizedZipCode + "/json/";

        try {
            ViaCepResponse response = restTemplate.getForObject(url, ViaCepResponse.class);
            return mapResponse(response, normalizedZipCode);
        } catch (RestClientException ex) {
            log.warn("Erro ao consultar ViaCEP para CEP {}", normalizedZipCode, ex);
            throw new BusinessException("Nao foi possivel consultar o CEP informado");
        }
    }

    private AddressLookupResponse mapResponse(ViaCepResponse response, String zipCode) {
        if (response == null || Boolean.TRUE.equals(response.getErro())) {
            throw new BusinessException("CEP nao encontrado: " + zipCode);
        }

        return AddressLookupResponse.builder()
                .zipCode(response.getCep())
                .street(response.getLogradouro())
                .neighborhood(response.getBairro())
                .city(response.getLocalidade())
                .state(response.getUf())
                .formattedAddress(formatAddress(response))
                .build();
    }

    private String formatAddress(ViaCepResponse response) {
        return String.join(", ",
                nullToBlank(response.getLogradouro()),
                nullToBlank(response.getBairro()),
                nullToBlank(response.getLocalidade()) + " - " + nullToBlank(response.getUf())
        ).replaceAll("(^,\\s*)|(,\\s*,)", "").trim();
    }

    private String normalizeZipCode(String zipCode) {
        String digits = zipCode == null ? "" : zipCode.replaceAll("\\D", "");
        if (digits.length() != 8) {
            throw new BusinessException("CEP deve conter 8 digitos");
        }
        return digits;
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ViaCepResponse {
        private String cep;
        private String logradouro;
        private String bairro;
        private String localidade;
        private String uf;

        @JsonProperty("erro")
        private Boolean erro;
    }
}
