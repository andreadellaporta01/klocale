package dev.klocale.testkit

import dev.klocale.NumberStyle

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
)
