package dev.klocale

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FormatStringTest {

    @Test
    fun invalidLiteralThrowsConsistently() {
        val formatter = NumberFormatter.orThrow(NumberStyle.Decimal(), NumberLocale.US)
        assertFailsWith<IllegalArgumentException> { formatter.format("abc") }
        assertFailsWith<IllegalArgumentException> { formatter.format("1,234") }
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
