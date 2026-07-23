package dev.klocale.internal

import dev.klocale.NormalizationPolicy
import dev.klocale.NumberFormatter
import dev.klocale.NumberLocale
import dev.klocale.NumberStyle

internal class NumberFormatterImpl(
    override val locale: NumberLocale,
    override val style: NumberStyle,
    override val policy: NormalizationPolicy,
    private val platform: PlatformFormatter,
) : NumberFormatter {

    override fun format(value: Double): String =
        OutputNormalizer.apply(platform.format(DecimalInput.OfDouble(value)), policy)

    override fun format(value: Long): String =
        OutputNormalizer.apply(platform.format(DecimalInput.OfLong(value)), policy)

    override fun format(value: String): String {
        require(DECIMAL_LITERAL.matches(value)) { "Not a valid decimal literal: '$value'" }
        return OutputNormalizer.apply(platform.format(DecimalInput.OfString(value)), policy)
    }

    private companion object {
        private val DECIMAL_LITERAL = Regex("[+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][+-]?\\d+)?")
    }
}
