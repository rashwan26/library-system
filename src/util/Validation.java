package util;

public final class Validation {
    private Validation() {}
    public static boolean booleanValue(String value) {
        if (!value.equals("true") && !value.equals("false")) throw new IllegalArgumentException("Invalid boolean field.");
        return value.equals("true");
    }
    public static String identifier(String value) {
        if (value == null || !value.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,63}"))
            throw new IllegalArgumentException("IDs must use 1-64 letters, digits, hyphens or underscores.");
        return value;
    }
    public static String text(String value, String label, boolean required) {
        if (value == null) value = "";
        if ((required && value.trim().isEmpty()) || value.length() > 2000 || value.contains("\n") || value.contains("\r"))
            throw new IllegalArgumentException("Invalid " + label + ": use a single line of at most 2000 characters.");
        return value;
    }
}
