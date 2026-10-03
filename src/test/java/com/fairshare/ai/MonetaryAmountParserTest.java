package com.fairshare.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
            "'paid 200 for dinner', 20000"
    })
    void parsesCommonInrFormatsAsPaise(String text, long expectedMinor) {
        assertEquals(expectedMinor, MonetaryAmountParser.toMinorUnits(text));
    }
}
