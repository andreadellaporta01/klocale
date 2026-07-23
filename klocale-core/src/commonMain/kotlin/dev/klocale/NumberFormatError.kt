package dev.klocale

/** Errors that can occur while *constructing* a [NumberFormatter]. Formatting itself never throws. */
public sealed class NumberFormatError(message: String) : Exception(message) {

    /** The language tag was not recognized by the platform locale database. */
    public class InvalidLocale(public val tag: String) :
        NumberFormatError("Unknown locale language tag: '$tag'")

    /** The ISO 4217 currency code was malformed or unknown. */
    public class InvalidCurrencyCode(public val code: String) :
        NumberFormatError("Invalid ISO 4217 currency code: '$code'")

    /** The requested fraction-digit range is illegal (negative, or min > max). */
    public class InvalidFractionRange(public val min: Int, public val max: Int) :
        NumberFormatError("Invalid fraction-digit range: min=$min, max=$max")

    /** The requested style is not supported by the backend on the current platform. */
    public class UnsupportedStyle(public val style: NumberStyle, public val backend: String) :
        NumberFormatError("Style $style is not supported by backend '$backend'")
}
