package com.gymiq.service;

import com.gymiq.security.PersonalDataProtection;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PersonalDataProtectionService {

    @Value("${gymiq.pii.encryption-key}")
    private String encryptionKey;

    @Value("${gymiq.pii.hash-secret}")
    private String hashSecret;

    @PostConstruct
    void configureProtection() {
        PersonalDataProtection.configure(encryptionKey, hashSecret);
    }

    public String emailHash(String email) {
        return PersonalDataProtection.emailHash(email);
    }

    public String cpfHash(String cpf) {
        return PersonalDataProtection.cpfHash(cpf);
    }

    public String normalizeSearchTerm(String term) {
        return PersonalDataProtection.normalizeSearchTerm(term);
    }
}
