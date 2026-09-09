package kr.ac.knue.commonfoundation.emailverification;

import java.util.Locale;
import java.util.regex.Pattern;

public final class EmailAddressPolicy {
    public static final String SUBSTITUTE_EMAIL = "admin@kndadmin.com";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private EmailAddressPolicy() {
    }

    public static String normalizeEmail(String email) {
        if (email == null) {
            throw new IllegalArgumentException("email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public static void validateSensitiveMailTarget(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (isSubstituteEmail(normalizedEmail)) {
            throw new IllegalArgumentException("substitute email cannot receive sensitive mail");
        }
    }

    public static boolean isSubstituteEmail(String email) {
        return SUBSTITUTE_EMAIL.equals(normalizeEmail(email));
    }

    public static boolean isValidEmailFormat(String email) {
        String normalizedEmail = normalizeEmail(email);
        return normalizedEmail.length() <= 320 && EMAIL_PATTERN.matcher(normalizedEmail).matches();
    }
}
