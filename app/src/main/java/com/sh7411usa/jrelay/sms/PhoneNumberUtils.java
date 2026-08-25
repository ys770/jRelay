package com.sh7411usa.jrelay.sms;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhoneNumberUtils {

    /**
     * Matches a leading phone number in any of the common US formats the app accepts
     * (some of which contain internal spaces, e.g. "+1 (234) 567-8910"), followed by
     * whitespace and the remaining text (a nickname).
     */
    private static final Pattern LEADING_NUMBER_PATTERN = Pattern.compile(
            "^(\\+1\\s?\\(\\d{3}\\)\\s?\\d{3}-?\\d{4}" +
                    "|\\+1\\d{10}" +
                    "|1-\\d{3}-\\d{3}-\\d{4}" +
                    "|\\(\\d{3}\\)\\s?\\d{3}-?\\d{4}" +
                    "|\\d{3}-\\d{3}-\\d{4}" +
                    "|\\d{10}" +
                    "|\\+\\d{8,15})" +
                    "\\s+(.+)$");

    public static String normalize(String rawInput) {
        if (rawInput == null) {
            return null;
        }
        String trimmed = rawInput.trim();
        boolean hasPlus = trimmed.startsWith("+");
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (Character.isDigit(c)) {
                digits.append(c);
            }
        }
        String d = digits.toString();

        if (hasPlus) {
            if (d.length() >= 11 && d.length() <= 15) {
                return "+" + d;
            }
            return null;
        }

        if (d.length() == 11 && d.charAt(0) == '1') {
            return "+" + d;
        }
        if (d.length() == 10) {
            return "+1" + d;
        }
        return null;
    }

    /**
     * Splits a "<number> <nickname>" argument string into the raw number token and the
     * remaining nickname, tolerating spaces inside the number itself. Returns null if no
     * recognizable phone number is found at the start of the text.
     */
    public static String[] splitLeadingNumberAndRest(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = LEADING_NUMBER_PATTERN.matcher(text.trim());
        if (matcher.find()) {
            return new String[]{matcher.group(1), matcher.group(2)};
        }
        return null;
    }
}
