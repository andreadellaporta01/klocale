package dev.klocale.internal

import dev.klocale.NumberStyle

internal data class LocaleInfo(
    val canonicalTag: String,
    val decimalSeparator: Char,
    val groupingSeparator: Char,
)

internal sealed interface DecimalInput {
    data class OfDouble(val value: Double) : DecimalInput
    data class OfLong(val value: Long) : DecimalInput
    data class OfString(val value: String) : DecimalInput
}

internal class FormatSpec(
    val localeTag: String,
    val style: NumberStyle,
)

internal interface PlatformFormatter {
    fun format(value: DecimalInput): String
}

internal expect val backendName: String

internal expect fun currentLocaleTag(): String

internal expect fun platformLocaleInfo(languageTag: String): LocaleInfo?

internal expect fun createPlatformFormatter(spec: FormatSpec): PlatformFormatter
