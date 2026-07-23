package dev.klocale

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FormatStringTest {

    @Test
    fun invalidLiteralThrowsConsistently() {
        val formatter = NumberFormatter.orThrow(NumberStyle.Decimal(), NumberLocale.US)
        assertFailsWith<IllegalArgumentException> { formatter.format("abc") }
        assertFailsWith<IllegalArgumentException> { formatter.format("1,234") }
    }

    @Test
    fun currencyCodeIsValidatedByShapeOnly() {
        val malformed = NumberFormatter(NumberStyle.Currency("US1"), NumberLocale.US)
        assertTrue(malformed.exceptionOrNull() is NumberFormatError.InvalidCurrencyCode)
        // Shape-valid but not a real ISO 4217 code: accepted (not checked against the registry).
        assertTrue(NumberFormatter(NumberStyle.Currency("XYZ"), NumberLocale.US).isSuccess)
    }

    @Test
    fun validLiteralFormats() {
        val formatter = NumberFormatter.orThrow(
            NumberStyle.Decimal(minFractionDigits = 2, maxFractionDigits = 2),
            NumberLocale.US,
        )
        assertEquals("1,234.56", formatter.format("1234.56"))
        assertEquals("-0.50", formatter.format("-.5"))
    }
}
