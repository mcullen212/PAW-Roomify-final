package ar.edu.itba.paw.model;

public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;

    private PasswordPolicy() {
        // Utility class.
    }

    public static boolean isValid(String password) {
        return password != null
                && hasMinimumLength(password)
                && hasNumber(password)
                && hasUppercase(password);
    }

    public static boolean hasMinimumLength(String password) {
        return password != null && password.length() >= MIN_LENGTH;
    }

    public static boolean hasNumber(String password) {
        return password != null && password.chars().anyMatch(Character::isDigit);
    }

    public static boolean hasUppercase(String password) {
        return password != null && password.chars().anyMatch(Character::isUpperCase);
    }
}
