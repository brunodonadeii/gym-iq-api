package com.gymiq.service;

import com.gymiq.dto.response.AddressLookupResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentDataService {

    private final CpfValidationService cpfValidationService;
    private final AddressLookupService addressLookupService;

    public void validateCpf(String cpf) {
        cpfValidationService.validate(cpf);
    }

    public String resolveAddress(String zipCode, String fallbackAddress) {
        if (zipCode == null || zipCode.isBlank()) {
            return fallbackAddress;
        }

        AddressLookupResponse address = addressLookupService.lookupByZipCode(zipCode);
        return address.getFormattedAddress();
    }
}
