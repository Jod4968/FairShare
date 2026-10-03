package com.fairshare.ai;

import com.fairshare.auth.InvalidRequestException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MonetaryAmountParser {
    private static final String NUMBER = "(\\d+(?:\\.\\d{1,2})?)";
    private static final Pattern CURRENCY_SYMBOL = Pattern.compile("(?<![\\w.])₹\\s*" + NUMBER + "(?![\\w.])");
    private static final Pattern RUPEE_PREFIX = Pattern.compile("(?<![\\w.])Rs\\.?\\s*" + NUMBER + "(?![\\w.])", Pattern.CASE_INSENSITIVE);
    private static final Pattern RUPEE_WORD = Pattern.compile("(?<![\\w.])" + NUMBER + "\\s*(?:rupees?|rs\\.?)" + "(?![\\w.])", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAID_AMOUNT = Pattern.compile("\\bpaid\\s+" + NUMBER + "(?![\\w.])", Pattern.CASE_INSENSITIVE);

    private MonetaryAmountParser() {}

    static long toMinorUnits(String text) {
        List<AmountMatch> matches = allMatches(text, CURRENCY_SYMBOL, RUPEE_PREFIX, RUPEE_WORD, PAID_AMOUNT);
        if (matches.isEmpty()) throw new InvalidRequestException("I couldn't find a valid rupee amount.");
        List<AmountMatch> distinctMatches = new ArrayList<>();
        for (AmountMatch match : matches) {
            if (distinctMatches.stream().noneMatch(existing -> overlaps(existing, match))) distinctMatches.add(match);
        }
        if (distinctMatches.size() > 1) throw new InvalidRequestException("Please provide exactly one rupee amount.");
        try {
            long amountMinor = new BigDecimal(distinctMatches.get(0).value()).movePointRight(2)
                    .setScale(0, RoundingMode.UNNECESSARY).longValueExact();
            if (amountMinor <= 0) throw new InvalidRequestException("Amount must be greater than zero.");
            return amountMinor;
        } catch (ArithmeticException exception) {
            throw new InvalidRequestException("Amount must be a positive rupee value with at most two decimal places.");
        }
    }

    private static List<AmountMatch> allMatches(String text, Pattern... patterns) {
        List<AmountMatch> matches = new ArrayList<>();
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) matches.add(new AmountMatch(matcher.start(), matcher.end(), matcher.group(1)));
        }
        return matches;
    }

    private static boolean overlaps(AmountMatch first, AmountMatch second) {
        return first.start() < second.end() && second.start() < first.end();
    }

    private record AmountMatch(int start, int end, String value) {}
}
