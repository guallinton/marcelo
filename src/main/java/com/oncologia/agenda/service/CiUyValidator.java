package com.oncologia.agenda.service;

public final class CiUyValidator {
    private static final int[] FACTORS = {2, 9, 8, 7, 6, 3, 4};

    private CiUyValidator() {
    }

    public static boolean isValid(String value) {
        String digits = clean(value);
        if (digits.length() < 7 || digits.length() > 8) {
            return false;
        }
        String body = digits.substring(0, digits.length() - 1);
        int checkDigit = Character.digit(digits.charAt(digits.length() - 1), 10);
        return validationDigit(body) == checkDigit;
    }

    public static String clean(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isDigit(c)) {
                digits.append(c);
            }
        }
        return digits.toString();
    }

    public static int validationDigit(String value) {
        String body = clean(value);
        while (body.length() < 7) {
            body = "0" + body;
        }
        if (body.length() > 7) {
            body = body.substring(body.length() - 7);
        }
        int sum = 0;
        for (int i = 0; i < FACTORS.length; i++) {
            sum += Character.digit(body.charAt(i), 10) * FACTORS[i];
        }
        int remainder = sum % 10;
        return remainder == 0 ? 0 : 10 - remainder;
    }
}
