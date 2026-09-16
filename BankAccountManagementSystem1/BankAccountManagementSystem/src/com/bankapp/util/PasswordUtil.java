package com.bankapp.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Handles secure PIN storage.
 * NON-FUNCTIONAL REQUIREMENT: Security — PINs are never stored in plain text.
 * Each PIN is combined with a unique random salt and hashed with SHA-256
 * before being persisted, so even if the data file is read directly,
 * the original PIN cannot be recovered.
 */
public final class PasswordUtil {

    private static final String ALGORITHM = "SHA-256";
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
        // utility class, no instances
    }

    /** Generates a fresh random salt, encoded as a Base64 string for easy storage. */
    public static String generateSalt() {
        byte[] saltBytes = new byte[16];
        RANDOM.nextBytes(saltBytes);
        return Base64.getEncoder().encodeToString(saltBytes);
    }

    /** Hashes the given PIN with the given salt, returning a Base64-encoded digest. */
    public static String hash(String rawPin, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            digest.update(Base64.getDecoder().decode(salt));
            byte[] hashed = digest.digest(rawPin.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            // SHA-256 and UTF-8 are guaranteed to be available on every JVM,
            // so this can only happen due to a misconfigured runtime.
            throw new IllegalStateException("Unable to hash PIN", e);
        }
    }

    /** Verifies a raw PIN attempt against a previously stored hash + salt. */
    public static boolean verify(String rawPin, String salt, String expectedHash) {
        String computed = hash(rawPin, salt);
        return constantTimeEquals(computed, expectedHash);
    }

    /**
     * Compares two strings in constant time to reduce the risk of
     * timing-based side-channel attacks on hash comparison.
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
