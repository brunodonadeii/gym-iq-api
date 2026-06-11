package com.gymiq.entity.converter;

import com.gymiq.security.PersonalDataProtection;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return PersonalDataProtection.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return PersonalDataProtection.decrypt(dbData);
    }
}
