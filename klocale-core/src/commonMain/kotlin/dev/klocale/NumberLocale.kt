package dev.klocale

import dev.klocale.internal.currentLocaleTag
import dev.klocale.internal.platformLocaleInfo

/**
 * A locale identified by a BCP-47 language tag (e.g. `"en-US"`, `"it-IT"`).
 *
 * Locale-dependent data (separators) is resolved once from the platform locale database and cached.
 */
public class NumberLocale internal constructor(public val languageTag: String) {
    private val info by lazy { platformLocaleInfo(languageTag) }

    /** The locale's decimal separator (e.g. `'.'` for en-US, `','` for it-IT). */
    public val decimalSeparator: Char
        get() = info?.decimalSeparator ?: '.'

    /** The locale's grouping separator (e.g. `','` for en-US, `'.'` for it-IT). */
    public val groupingSeparator: Char
        get() = info?.groupingSeparator ?: ','

    override fun equals(other: Any?): Boolean = other is NumberLocale && other.languageTag == languageTag

    override fun hashCode(): Int = languageTag.hashCode()

    override fun toString(): String = "NumberLocale($languageTag)"

    public companion object {
        /** The current system locale. */
        public fun current(): NumberLocale = NumberLocale(currentLocaleTag())

        /**
         * Builds a locale from a BCP-47 [tag], returning a failure with
         * [NumberFormatError.InvalidLocale] if the platform does not recognize it.
         */
        public fun fromLanguageTag(tag: String): Result<NumberLocale> =
            if (platformLocaleInfo(tag) != null) {
                Result.success(NumberLocale(tag))
            } else {
                Result.failure(NumberFormatError.InvalidLocale(tag))
            }

        public val US: NumberLocale = NumberLocale("en-US")
        public val UK: NumberLocale = NumberLocale("en-GB")
        public val ITALY: NumberLocale = NumberLocale("it-IT")
        public val GERMANY: NumberLocale = NumberLocale("de-DE")
        public val FRANCE: NumberLocale = NumberLocale("fr-FR")
        public val SPAIN: NumberLocale = NumberLocale("es-ES")
        public val JAPAN: NumberLocale = NumberLocale("ja-JP")
        public val INDIA: NumberLocale = NumberLocale("en-IN")
        public val CHINA: NumberLocale = NumberLocale("zh-CN")
        public val BRAZIL: NumberLocale = NumberLocale("pt-BR")
    }
}
