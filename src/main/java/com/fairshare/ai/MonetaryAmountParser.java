package com.fairshare.ai;

import com.fairshare.auth.InvalidRequestException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MonetaryAmountParser {
    private static final String NUMBER = "(\\d+(?:\\.\\d{1,2})?)";
    private static final Pattern CURRENCY_SYMBOL = Pattern.compile("₹\\s*" + NUMBER);
    private static final Pattern RUPEE_PREFIX = Pattern.compile("\\bRs\\.?\\s*" + NUMBER, Pattern.CASE_INSENSITIVE);
    private static final Pattern RUPEE_WORD = Pattern.compile(NUMBER + "\\s*(?:rupees?|rs\\.?)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAID_AMOUNT = Pattern.compile("\\bpaid\\s+" + NUMBER + "\\b", Pattern.CASE_INSENSITIVE);

    private MonetaryAmountParser() {}

    static long toMinorUnits(String text) {
        Matcher matcher = firstMatch(text, CURRENCY_SYMBOL, RUPEE_PREFIX, RUPEE_WORD, PAID_AMOUNT);
        if (matcher == null) throw new InvalidRequestException("I couldn't find a valid rupee amount.");
        try {
            long amountMinor = new BigDecimal(matcher.group(1)).movePointRight(2)
                    .setScale(0, RoundingMode.UNNECESSARY).longValueExact();
            if (amountMinor <= 0) throw new InvalidRequestException("Amount must be greater than zero.");
            return amountMinor;
        } catch (ArithmeticException exception) {
            throw new InvalidRequestException("Amount must be a positive rupee value with at most two decimal places.");
        }
    }

    private static Matcher firstMatch(String text, Pattern... patterns) {
        Matcher best = null;
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find() && (best == null || matcher.start() < best.start())) best = matcher;
        }
        return best;
    }
}
