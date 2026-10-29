package com.library.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility for hashing and verifying passwords securely using SHA-256.
 * [Concept: Security and Utility Design]
 */
public final class PasswordUtil {

    private PasswordUtil() {
    }

    /**
     * Computes the SHA-256 hex digest of a plain-text password.
     *
     * @param plainText cleartext password
     * @return 64-character lowercase hexadecimal hash
     */
    public static String hash(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(plainText.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm unavailable", e);
        }
    }

    /**
     * Verifies whether a candidate password matches a stored hash.
     */
    public static boolean verify(String candidatePassword, String storedHash) {
        if (candidatePassword == null || storedHash == null) {
            return false;
        }
        return hash(candidatePassword).equalsIgnoreCase(storedHash);
    }
}
