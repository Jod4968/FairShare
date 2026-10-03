package com.fairshare.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MonetaryAmountParserTest {
    @ParameterizedTest
    @CsvSource({
            "'₹200', 20000",
            "'₹200.50', 20050",
            "'200 rupees', 20000",
            "'200.50 rupees', 20050",
            "'200 rs', 20000",
            "'200.50 rs', 20050",
            "'200rs', 20000",
            "'200.50rs', 20050",
            "'Rs 200', 20000",
            "'Rs. 200', 20000",
            "'Rs 200.50', 20050",
            "'Rs. 200.50', 20050",
            "'paid 200 for dinner', 20000",
            "'I paid 1000 for lunch', 100000",
            "'I paid 1000rs for lunch', 100000",
            "'I paid ₹1000 for lunch', 100000",
            "'I paid 1000 rupees for lunch', 100000",
            "'I paid 1000.50 for lunch', 100050",
            "'I paid ₹1000.50 for lunch', 100050",
            "'I paid 200 for both of us', 20000",
            "'I paid 1000.5 for lunch', 100050"
    })
    void parsesCommonInrFormatsAsPaise(String text, long expectedMinor) {
        assertEquals(expectedMinor, MonetaryAmountParser.toMinorUnits(text));
    }

    @org.junit.jupiter.api.Test
    void rejectsMalformedAmountsInsteadOfTruncatingThem() {
        assertThrows(RuntimeException.class, () -> MonetaryAmountParser.toMinorUnits("I paid 1000.123 for lunch"));
        assertThrows(RuntimeException.class, () -> MonetaryAmountParser.toMinorUnits("I paid ₹1000."));
        assertThrows(RuntimeException.class, () -> MonetaryAmountParser.toMinorUnits("I paid Rs. for lunch"));
        assertThrows(RuntimeException.class, () -> MonetaryAmountParser.toMinorUnits("I paid ₹1000 and ₹200 for lunch"));
    }
}
