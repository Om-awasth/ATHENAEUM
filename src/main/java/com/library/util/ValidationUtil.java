package com.library.util;

import com.library.exception.InvalidISBNException;

import java.util.regex.Pattern;

/**
 * Utility for domain-level data validation including ISBN check-digits, email formats, and string rules.
 * [Concept: Defensive Programming and Input Validation]
 */
public final class ValidationUtil {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private ValidationUtil() {
    }

    /**
     * Validates an ISBN-10 or ISBN-13 string.
     * Strips hyphens and checks valid length and numeric validity.
     * Throws InvalidISBNException if invalid.
     *
     * @param isbn input string (e.g., "978-0134685991")
     * @throws InvalidISBNException if the ISBN is malformed
     */
    public static void validateISBN(String isbn) throws InvalidISBNException {
        if (isbn == null || isbn.isBlank()) {
            throw new InvalidISBNException(isbn, "ISBN cannot be null or empty");
        }
        String clean = isbn.replaceAll("[-\\s]", "");
        if (clean.length() == 10) {
            // ISBN-10 checksum validation
            int sum = 0;
            for (int i = 0; i < 9; i++) {
                char ch = clean.charAt(i);
                if (!Character.isDigit(ch)) {
                    throw new InvalidISBNException(isbn, "ISBN-10 contains non-numeric character: " + ch);
                }
                sum += (ch - '0') * (10 - i);
            }
            char last = clean.charAt(9);
            if (last == 'X' || last == 'x') {
                sum += 10;
            } else if (Character.isDigit(last)) {
                sum += (last - '0');
            } else {
                throw new InvalidISBNException(isbn, "Invalid check digit in ISBN-10: " + last);
            }
            if (sum % 11 != 0) {
                throw new InvalidISBNException(isbn, "ISBN-10 checksum failure for: " + isbn);
            }
        } else if (clean.length() == 13) {
            // ISBN-13 checksum validation
            int sum = 0;
            for (int i = 0; i < 12; i++) {
                char ch = clean.charAt(i);
                if (!Character.isDigit(ch)) {
                    throw new InvalidISBNException(isbn, "ISBN-13 contains non-numeric character: " + ch);
                }
                int digit = ch - '0';
                sum += (i % 2 == 0) ? digit : digit * 3;
            }
            int remainder = sum % 10;
            int checkDigit = (remainder == 0) ? 0 : 10 - remainder;
            char last = clean.charAt(12);
            if (!Character.isDigit(last) || (last - '0') != checkDigit) {
                throw new InvalidISBNException(isbn, "ISBN-13 checksum failure for: " + isbn);
            }
        } else {
            throw new InvalidISBNException(isbn, "ISBN must be either 10 or 13 digits (found " + clean.length() + ")");
        }
    }

    /**
     * Validates standard email address format.
     */
    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }
}
