package com.gymiq.service;

import com.gymiq.dto.response.AddressLookupResponse;
import com.gymiq.integration.ViaCepClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AddressLookupService {

    private final ViaCepClient viaCepClient;

    public AddressLookupResponse lookupByZipCode(String zipCode) {
        return viaCepClient.lookup(zipCode);
    }
}
