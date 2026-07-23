package dev.klocale.testkit

import dev.klocale.MeasureUnit
import dev.klocale.NumberStyle
import dev.klocale.RoundingMode
import dev.klocale.SignDisplay
import dev.klocale.TimeUnit

/** The runtime backend a test is executing on. */
enum class Platform { ANDROID, JVM, IOS, MACOS, JS, WASM_JS }

/** Set by the running test target; used to apply per-platform [GoldenCase.overrides]. */
expect val currentPlatform: Platform

/**
 * A single (locale, style, input) → expected-output expectation, run against every backend.
 *
 * [expected] is the output *after* the default [dev.klocale.NormalizationPolicy], i.e. ASCII minus
 * and non-breaking (U+00A0) grouping spaces. When a backend legitimately diverges (e.g. differing
 * CLDR versions), record the platform-specific string in [overrides] with a documented reason.
 */
data class GoldenCase(
    val id: String,
    val locale: String,
    val style: NumberStyle,
    val input: String,
    val expected: String,
    val overrides: Map<Platform, String> = emptyMap(),
    val unsupportedOn: Set<Platform> = emptySet(),
) {
    fun expectedFor(platform: Platform): String = overrides[platform] ?: expected
}

private val dec2 = NumberStyle.Decimal(minFractionDigits = 2, maxFractionDigits = 2)
private val dec3 = NumberStyle.Decimal()
private val decNoGroup = NumberStyle.Decimal(minFractionDigits = 2, maxFractionDigits = 2, grouping = false)

/** The shared expectation table. Kept to CLDR-stable locales/inputs for the core set. */
val GOLDEN_CASES: List<GoldenCase> = listOf(
    GoldenCase("dec_en_us", "en-US", dec2, "1234.56", "1,234.56"),
    GoldenCase("dec_it_it", "it-IT", dec2, "1234567.89", "1.234.567,89"),
    GoldenCase("dec_de_de", "de-DE", dec2, "1234.56", "1.234,56"),
    GoldenCase("dec_fr_fr", "fr-FR", dec2, "1234.56", "1\u00A0234,56"),
    GoldenCase("dec_en_in", "en-IN", dec2, "1234567.5", "12,34,567.50"),
    GoldenCase("dec_en_us_neg", "en-US", dec2, "-1234.56", "-1,234.56"),
    GoldenCase("dec_en_us_3frac", "en-US", dec3, "1234.5", "1,234.5"),
    GoldenCase("dec_en_us_nogroup", "en-US", decNoGroup, "1234.56", "1234.56"),
    GoldenCase("dec_en_us_zero", "en-US", dec2, "0", "0.00"),
    GoldenCase("cur_en_us_usd", "en-US", NumberStyle.Currency("USD"), "1234.5", "\$1,234.50"),
    GoldenCase("cur_en_us_eur", "en-US", NumberStyle.Currency("EUR"), "1234.5", "€1,234.50"),
    GoldenCase("cur_de_de_eur", "de-DE", NumberStyle.Currency("EUR"), "1234.5", "1.234,50\u00A0€"),
    GoldenCase("pct_en_us", "en-US", NumberStyle.Percent(), "0.42", "42%"),
    GoldenCase("pct_en_us_frac", "en-US", NumberStyle.Percent(minFractionDigits = 1, maxFractionDigits = 1), "0.425", "42.5%"),
    GoldenCase("pct_de_de", "de-DE", NumberStyle.Percent(), "0.42", "42\u00A0%"),
    GoldenCase("pct_value", "en-US", NumberStyle.Percent(scale = NumberStyle.Percent.Scale.VALUE), "42", "42%"),
    GoldenCase("sci_en_us", "en-US", NumberStyle.Scientific(), "12345", "1.2345E4"),
    GoldenCase("sci_small", "en-US", NumberStyle.Scientific(), "0.00012345", "1.2345E-4"),
    GoldenCase("cmp_en_k", "en-US", NumberStyle.Compact(), "1200", "1.2K"),
    GoldenCase("cmp_en_k2", "en-US", NumberStyle.Compact(), "12000", "12K"),
    GoldenCase("cmp_en_m", "en-US", NumberStyle.Compact(), "1200000", "1.2M"),
    GoldenCase("cmp_en_b", "en-US", NumberStyle.Compact(), "1200000000", "1.2B"),
    GoldenCase("cmp_en_t", "en-US", NumberStyle.Compact(), "1200000000000", "1.2T"),
    GoldenCase("cmp_en_small", "en-US", NumberStyle.Compact(), "999", "999"),
    GoldenCase("ord_en_1", "en-US", NumberStyle.Ordinal(), "1", "1st"),
    GoldenCase("ord_en_2", "en-US", NumberStyle.Ordinal(), "2", "2nd"),
    GoldenCase("ord_en_3", "en-US", NumberStyle.Ordinal(), "3", "3rd"),
    GoldenCase("ord_en_4", "en-US", NumberStyle.Ordinal(), "4", "4th"),
    GoldenCase("ord_en_11", "en-US", NumberStyle.Ordinal(), "11", "11th"),
    GoldenCase("ord_en_21", "en-US", NumberStyle.Ordinal(), "21", "21st"),
    GoldenCase("ord_en_22", "en-US", NumberStyle.Ordinal(), "22", "22nd"),
    GoldenCase("ord_en_23", "en-US", NumberStyle.Ordinal(), "23", "23rd"),
    GoldenCase("spell_en_123", "en-US", NumberStyle.Spellout(), "123", "one hundred twenty-three", unsupportedOn = setOf(Platform.JS, Platform.WASM_JS, Platform.ANDROID)),
    GoldenCase("spell_en_million", "en-US", NumberStyle.Spellout(), "1000000", "one million", unsupportedOn = setOf(Platform.JS, Platform.WASM_JS, Platform.ANDROID)),
    GoldenCase("spell_en_neg", "en-US", NumberStyle.Spellout(), "-5", "minus five", unsupportedOn = setOf(Platform.JS, Platform.WASM_JS, Platform.ANDROID)),
    GoldenCase("rel_en_in3d", "en-US", NumberStyle.RelativeTime(TimeUnit.DAY, numeric = NumberStyle.RelativeTime.Numeric.ALWAYS), "3", "in 3 days"),
    GoldenCase("rel_en_2dago", "en-US", NumberStyle.RelativeTime(TimeUnit.DAY, numeric = NumberStyle.RelativeTime.Numeric.ALWAYS), "-2", "2 days ago"),
    GoldenCase("meas_en_km", "en-US", NumberStyle.Measure(MeasureUnit.KILOMETER), "12.5", "12.5 km", unsupportedOn = setOf(Platform.MACOS, Platform.IOS)),
    GoldenCase("cur_en_zero", "en-US", NumberStyle.Currency("USD"), "0", "\$0.00"),
    GoldenCase("cur_en_accounting_neg", "en-US", NumberStyle.Currency("USD", presentation = NumberStyle.Currency.Presentation.ACCOUNTING), "-1234.5", "(\$1,234.50)"),
    GoldenCase("pct_en_zero", "en-US", NumberStyle.Percent(), "0", "0%"),
    GoldenCase("cmp_en_neg", "en-US", NumberStyle.Compact(), "-1200", "-1.2K"),
    GoldenCase("cmp_en_1k", "en-US", NumberStyle.Compact(), "1000", "1K"),
    GoldenCase("sci_en_neg", "en-US", NumberStyle.Scientific(), "-12345", "-1.2345E4"),
    GoldenCase("ord_en_0", "en-US", NumberStyle.Ordinal(), "0", "0th"),
    GoldenCase("rnd_halfup", "en-US", NumberStyle.Decimal(minFractionDigits = 0, maxFractionDigits = 0, rounding = RoundingMode.HALF_UP), "2.5", "3"),
    GoldenCase("rnd_halfdown", "en-US", NumberStyle.Decimal(minFractionDigits = 0, maxFractionDigits = 0, rounding = RoundingMode.HALF_DOWN), "2.5", "2"),
    GoldenCase("rnd_up", "en-US", NumberStyle.Decimal(minFractionDigits = 0, maxFractionDigits = 0, rounding = RoundingMode.UP), "2.1", "3"),
    GoldenCase("rnd_down", "en-US", NumberStyle.Decimal(minFractionDigits = 0, maxFractionDigits = 0, rounding = RoundingMode.DOWN), "2.9", "2"),
    GoldenCase("rnd_ceil_neg", "en-US", NumberStyle.Decimal(minFractionDigits = 0, maxFractionDigits = 0, rounding = RoundingMode.CEILING), "-2.1", "-2"),
    GoldenCase("rnd_floor_neg", "en-US", NumberStyle.Decimal(minFractionDigits = 0, maxFractionDigits = 0, rounding = RoundingMode.FLOOR), "-2.1", "-3"),
    GoldenCase("cur_en_usd_neg", "en-US", NumberStyle.Currency("USD"), "-1234.5", "-\$1,234.50"),
    GoldenCase("pct_en_neg", "en-US", NumberStyle.Percent(), "-0.42", "-42%"),
    GoldenCase("cmp_en_rollover_m", "en-US", NumberStyle.Compact(), "999999", "1M"),
    GoldenCase("cmp_en_rollover_b", "en-US", NumberStyle.Compact(), "999999999", "1B"),
    GoldenCase("dec_en_halfeven_down", "en-US", NumberStyle.Decimal(minFractionDigits = 0, maxFractionDigits = 0), "2.5", "2"),
    GoldenCase("dec_en_halfeven_up", "en-US", NumberStyle.Decimal(minFractionDigits = 0, maxFractionDigits = 0), "3.5", "4"),
    GoldenCase("dec_en_large", "en-US", NumberStyle.Decimal(minFractionDigits = 0, maxFractionDigits = 0), "1234567890", "1,234,567,890"),
    GoldenCase("dec_en_sign_always_pos", "en-US", NumberStyle.Decimal(minFractionDigits = 2, maxFractionDigits = 2, signDisplay = SignDisplay.ALWAYS), "1234.56", "+1,234.56"),
    GoldenCase("dec_en_sign_always_neg", "en-US", NumberStyle.Decimal(minFractionDigits = 2, maxFractionDigits = 2, signDisplay = SignDisplay.ALWAYS), "-1234.56", "-1,234.56"),
    GoldenCase("dec_en_sign_never", "en-US", NumberStyle.Decimal(minFractionDigits = 2, maxFractionDigits = 2, signDisplay = SignDisplay.NEVER), "-1234.56", "1,234.56"),
)
