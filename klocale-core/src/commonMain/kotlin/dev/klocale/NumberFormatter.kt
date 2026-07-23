package dev.klocale

import dev.klocale.internal.FormatSpec
import dev.klocale.internal.NumberFormatterImpl
import dev.klocale.internal.createPlatformFormatter
import dev.klocale.internal.platformLocaleInfo

/**
 * A reusable formatter bound to a [locale], [style] and [policy].
 *
 * Construction validates the configuration and can fail (see [invoke]); once built,
 * formatting a finite number **never** throws. Non-finite values are rendered as
 * documented tokens rather than raising.
 *
 * **Thread safety:** an instance may be reused freely, but is **not** safe for concurrent
 * formatting from multiple threads (it wraps a non-thread-safe platform formatter). Confine
 * a formatter to a single thread, or build one per thread.
 */
public interface NumberFormatter {
    public val locale: NumberLocale
    public val style: NumberStyle
    public val policy: NormalizationPolicy

    public fun format(value: Double): String
    public fun format(value: Long): String

    /**
     * Formats a decimal literal (e.g. `"123456789012345.99"`).
     *
     * Full precision is preserved for [NumberStyle.Decimal] and [NumberStyle.Currency] on the
     * JVM and Android backends; other styles and the Apple/JS/WasmJs backends format via [Double]
     * (best effort). Throws [IllegalArgumentException] if [value] is not a valid decimal literal.
     */
    public fun format(value: String): String

    public companion object {
        /**
         * Builds a formatter, returning a failed [Result] with a [NumberFormatError] when the
         * locale, currency code, fraction range or style is invalid on this platform.
         */
        public operator fun invoke(
            style: NumberStyle,
            locale: NumberLocale = NumberLocale.current(),
            policy: NormalizationPolicy = NormalizationPolicy.Default,
        ): Result<NumberFormatter> = runCatching {
            val info = platformLocaleInfo(locale.languageTag)
                ?: throw NumberFormatError.InvalidLocale(locale.languageTag)
            validate(style)
            val platform = createPlatformFormatter(FormatSpec(info.canonicalTag, style))
            NumberFormatterImpl(locale, style, policy, platform)
        }

        /** Like [invoke] but throws the [NumberFormatError] instead of returning a [Result]. */
        public fun orThrow(
            style: NumberStyle,
            locale: NumberLocale = NumberLocale.current(),
            policy: NormalizationPolicy = NormalizationPolicy.Default,
        ): NumberFormatter = invoke(style, locale, policy).getOrThrow()
    }
}

private fun validate(style: NumberStyle) {
    when (style) {
        is NumberStyle.Decimal -> checkFraction(style.minFractionDigits, style.maxFractionDigits)
        is NumberStyle.Percent -> checkFraction(style.minFractionDigits, style.maxFractionDigits)
        is NumberStyle.Currency -> {
            checkCurrency(style.currencyCode)
            val min = style.minFractionDigits
            val max = style.maxFractionDigits
            if (min != null && max != null) checkFraction(min, max)
        }
        else -> Unit
    }
}

private fun checkFraction(min: Int, max: Int) {
    if (min < 0 || max < 0 || min > max) throw NumberFormatError.InvalidFractionRange(min, max)
}

private fun checkCurrency(code: String) {
    if (code.length != 3 || !code.all { it in 'A'..'Z' }) {
        throw NumberFormatError.InvalidCurrencyCode(code)
    }
}

/** Convenience: format [value] as a plain decimal in [locale]. */
public fun formatDecimal(value: Double, locale: NumberLocale = NumberLocale.current()): String =
    NumberFormatter.orThrow(NumberStyle.Decimal(), locale).format(value)

/** Convenience: format [value] as [currencyCode] currency in [locale]. */
public fun formatCurrency(value: Double, currencyCode: String, locale: NumberLocale = NumberLocale.current()): String =
    NumberFormatter.orThrow(NumberStyle.Currency(currencyCode), locale).format(value)

/** Convenience: format a ratio [value] (0.42 → 42%) in [locale]. */
public fun formatPercent(value: Double, locale: NumberLocale = NumberLocale.current()): String =
    NumberFormatter.orThrow(NumberStyle.Percent(), locale).format(value)

/** Convenience: format [value] in compact notation (1200 → 1.2K) in [locale]. */
public fun formatCompact(value: Double, locale: NumberLocale = NumberLocale.current()): String =
    NumberFormatter.orThrow(NumberStyle.Compact(), locale).format(value)
