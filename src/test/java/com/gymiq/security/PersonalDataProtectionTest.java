package com.gymiq.security;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersonalDataProtectionTest {

    private static final String ENCRYPTION_SECRET = "12345678901234567890123456789012";
    private static final String HASH_SECRET = "abcdefghijklmnopqrstuvwxyz123456";

    @Test
    void configureShouldRejectShortSecrets() {
        assertThatThrownBy(() -> PersonalDataProtection.configure("short", HASH_SECRET))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PII_ENCRYPTION_KEY");

        assertThatThrownBy(() -> PersonalDataProtection.configure(ENCRYPTION_SECRET, "short"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PII_HASH_SECRET");
    }

    @Test
    void encryptAndDecryptShouldPreservePlainText() {
        PersonalDataProtection.configure(ENCRYPTION_SECRET, HASH_SECRET);

        String encrypted = PersonalDataProtection.encrypt("ana@gymiq.com");

        assertThat(encrypted).isNotEqualTo("ana@gymiq.com");
        assertThat(PersonalDataProtection.decrypt(encrypted)).isEqualTo("ana@gymiq.com");
        assertThat(PersonalDataProtection.encrypt("")).isEmpty();
        assertThat(PersonalDataProtection.decrypt(null)).isNull();
    }

    @Test
    void localDateEncryptionShouldRoundTrip() {
        PersonalDataProtection.configure(ENCRYPTION_SECRET, HASH_SECRET);

        String encrypted = PersonalDataProtection.encryptLocalDate(LocalDate.of(2000, 1, 15));

        assertThat(PersonalDataProtection.decryptLocalDate(encrypted)).isEqualTo(LocalDate.of(2000, 1, 15));
        assertThat(PersonalDataProtection.encryptLocalDate(null)).isNull();
        assertThat(PersonalDataProtection.decryptLocalDate("")).isNull();
    }

    @Test
    void hashesShouldNormalizeEmailAndCpf() {
        PersonalDataProtection.configure(ENCRYPTION_SECRET, HASH_SECRET);

        assertThat(PersonalDataProtection.emailHash(" Ana@GymIQ.com "))
                .isEqualTo(PersonalDataProtection.emailHash("ana@gymiq.com"));
        assertThat(PersonalDataProtection.cpfHash("123.456.789-09"))
                .isEqualTo(PersonalDataProtection.cpfHash("12345678909"));
    }

    @Test
    void masksShouldHideSensitiveData() {
        assertThat(PersonalDataProtection.maskEmail("ana.silva@gymiq.com")).isEqualTo("an***@gymiq.com");
        assertThat(PersonalDataProtection.maskEmail("x@gymiq.com")).isEqualTo("x***@gymiq.com");
        assertThat(PersonalDataProtection.maskEmail("email-invalido")).isEqualTo("email-invalido");
        assertThat(PersonalDataProtection.maskCpf("123.456.789-09")).isEqualTo("***.***.***-09");
        assertThat(PersonalDataProtection.maskCpf("123")).isEqualTo("***.***.***-**");
        assertThat(PersonalDataProtection.maskPhone("11987654321")).isEqualTo("***4321");
        assertThat(PersonalDataProtection.maskPhone("1234")).isEqualTo("1234");
        assertThat(PersonalDataProtection.normalizeSearchTerm(" João Ávila ")).isEqualTo("joao avila");
    }
}
