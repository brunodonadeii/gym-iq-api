package com.gymiq.entity.converter;

import com.gymiq.security.PersonalDataProtection;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDate;

@Converter
public class EncryptedLocalDateConverter implements AttributeConverter<LocalDate, String> {

    @Override
    public String convertToDatabaseColumn(LocalDate attribute) {
        return PersonalDataProtection.encryptLocalDate(attribute);
    }

    @Override
    public LocalDate convertToEntityAttribute(String dbData) {
        return PersonalDataProtection.decryptLocalDate(dbData);
    }
}
