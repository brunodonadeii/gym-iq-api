package com.gymiq.security;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

public final class PersonalDataProtection {

    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static volatile SecretKeySpec encryptionKey;
    private static volatile SecretKeySpec hashKey;

    private PersonalDataProtection() {
    }

    public static void configure(String encryptionSecret, String hashSecret) {
        encryptionKey = new SecretKeySpec(sha256(validateSecret(encryptionSecret, "PII_ENCRYPTION_KEY")), "AES");
        hashKey = new SecretKeySpec(validateSecret(hashSecret, "PII_HASH_SECRET").getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
    }

    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return plainText;
        }
        ensureConfigured();
        try {
            byte[] iv = new byte[IV_BYTES];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer payload = ByteBuffer.allocate(iv.length + encrypted.length);
            payload.put(iv);
            payload.put(encrypted);
            return Base64.getEncoder().encodeToString(payload.array());
        } catch (Exception exception) {
            throw new IllegalStateException("Nao foi possivel criptografar dado sensivel", exception);
        }
    }

    public static String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isBlank()) {
            return encryptedText;
        }
        ensureConfigured();
        try {
            byte[] payload = Base64.getDecoder().decode(encryptedText);
            ByteBuffer buffer = ByteBuffer.wrap(payload);

            byte[] iv = new byte[IV_BYTES];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("Nao foi possivel descriptografar dado sensivel", exception);
        }
    }

    public static String encryptLocalDate(LocalDate date) {
        return date == null ? null : encrypt(date.toString());
    }

    public static LocalDate decryptLocalDate(String encryptedDate) {
        String plainDate = decrypt(encryptedDate);
        return plainDate == null || plainDate.isBlank() ? null : LocalDate.parse(plainDate);
    }

    public static String emailHash(String email) {
        return hmac(normalizeEmail(email));
    }

    public static String cpfHash(String cpf) {
        return hmac(normalizeCpf(cpf));
    }

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    public static String normalizeCpf(String cpf) {
        return cpf == null ? "" : cpf.replaceAll("\\D", "");
    }

    public static String normalizeSearchTerm(String term) {
        if (term == null) {
            return "";
        }
        String normalized = Normalizer.normalize(term.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.toLowerCase(Locale.ROOT);
    }

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String visible = local.length() <= 2 ? local.substring(0, 1) : local.substring(0, 2);
        return visible + "***@" + parts[1];
    }

    public static String maskCpf(String cpf) {
        String digits = normalizeCpf(cpf);
        if (digits.length() != 11) {
            return "***.***.***-**";
        }
        return "***.***.***-" + digits.substring(9);
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() <= 4) {
            return phone;
        }
        return "***" + phone.substring(phone.length() - 4);
    }

    private static String hmac(String value) {
        ensureConfigured();
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(hashKey);
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Nao foi possivel gerar hash de dado sensivel", exception);
        }
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 nao esta disponivel", exception);
        }
    }

    private static String validateSecret(String value, String propertyName) {
        if (value == null || value.isBlank() || value.length() < 32) {
            throw new IllegalStateException(propertyName + " deve ser configurada com pelo menos 32 caracteres");
        }
        return value;
    }

    private static void ensureConfigured() {
        if (encryptionKey == null || hashKey == null) {
            throw new IllegalStateException("Protecao de dados pessoais nao configurada");
        }
    }
}
