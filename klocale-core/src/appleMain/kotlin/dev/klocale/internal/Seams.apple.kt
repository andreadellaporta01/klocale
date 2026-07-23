@file:OptIn(ExperimentalForeignApi::class)

package dev.klocale.internal

import dev.klocale.NumberFormatError
import dev.klocale.NumberStyle
import dev.klocale.RoundingMode
import dev.klocale.SignDisplay
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.round
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import platform.Foundation.NSDecimalNumber
import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterCurrencyAccountingStyle
import platform.Foundation.NSNumberFormatterCurrencyISOCodeStyle
import platform.Foundation.NSNumberFormatterCurrencyStyle
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.NSNumberFormatterOrdinalStyle
import platform.Foundation.NSNumberFormatterPercentStyle
import platform.Foundation.NSNumberFormatterScientificStyle
import platform.Foundation.NSNumberFormatterSpellOutStyle
import platform.Foundation.NSDateComponents
import platform.Foundation.NSRelativeDateTimeFormatter
import platform.Foundation.NSRelativeDateTimeFormatterStyleNamed
import platform.Foundation.NSRelativeDateTimeFormatterStyleNumeric
import platform.Foundation.NSRelativeDateTimeFormatterUnitsStyleAbbreviated
import platform.Foundation.NSRelativeDateTimeFormatterUnitsStyleFull
import platform.Foundation.NSRelativeDateTimeFormatterUnitsStyleShort
import dev.klocale.TimeUnit
import platform.Foundation.NSNumberFormatterRoundCeiling
import platform.Foundation.NSNumberFormatterRoundDown
import platform.Foundation.NSNumberFormatterRoundFloor
import platform.Foundation.NSNumberFormatterRoundHalfDown
import platform.Foundation.NSNumberFormatterRoundHalfEven
import platform.Foundation.NSNumberFormatterRoundHalfUp
import platform.Foundation.NSNumberFormatterRoundUp
import platform.Foundation.NSNumberFormatterRoundingMode
import platform.Foundation.currentLocale
import platform.Foundation.localeIdentifier

internal actual val backendName: String = "NSNumberFormatter"

internal actual fun currentLocaleTag(): String =
    NSLocale.currentLocale.localeIdentifier.replace('_', '-')

internal actual fun platformLocaleInfo(languageTag: String): LocaleInfo? {
    if (!looksLikeLanguageTag(languageTag)) return null
    val id = languageTag.replace('-', '_')
    val locale = NSLocale(localeIdentifier = id)
    val probe = NSNumberFormatter().apply {
        this.locale = locale
        numberStyle = NSNumberFormatterDecimalStyle
    }
    return LocaleInfo(
        canonicalTag = locale.localeIdentifier.replace('_', '-'),
        decimalSeparator = probe.decimalSeparator?.firstOrNull() ?: '.',
        groupingSeparator = probe.groupingSeparator?.firstOrNull() ?: ',',
    )
}

internal actual fun createPlatformFormatter(spec: FormatSpec): PlatformFormatter {
    return when (val style = spec.style) {
        is NumberStyle.Decimal -> DecimalAppleFormatter(spec.localeTag, style)
        is NumberStyle.Currency -> CurrencyAppleFormatter(spec.localeTag, style)
        is NumberStyle.Percent -> PercentAppleFormatter(spec.localeTag, style)
        is NumberStyle.Scientific -> ScientificAppleFormatter(spec.localeTag, style)
        is NumberStyle.Compact -> {
            val language = spec.localeTag.substringBefore('-').lowercase()
            if (language == "en" && style.length == NumberStyle.Compact.Length.SHORT) {
                CompactAppleFormatter(style)
            } else {
                throw NumberFormatError.UnsupportedStyle(style, backendName)
            }
        }
        is NumberStyle.Ordinal ->
            if (style.kind == NumberStyle.Ordinal.Kind.SUFFIX) OrdinalAppleFormatter(spec.localeTag) else throw NumberFormatError.UnsupportedStyle(style, backendName)
        is NumberStyle.Spellout -> SpelloutAppleFormatter(spec.localeTag)
        is NumberStyle.RelativeTime ->
            if (style.unit == TimeUnit.QUARTER) throw NumberFormatError.UnsupportedStyle(style, backendName) else RelativeTimeAppleFormatter(spec.localeTag, style)
        else -> throw NumberFormatError.UnsupportedStyle(style, backendName)
    }
}

private class RelativeTimeAppleFormatter(
    localeTag: String,
    private val style: NumberStyle.RelativeTime,
) : PlatformFormatter {

    private val fmt = NSRelativeDateTimeFormatter().apply {
        locale = NSLocale(localeIdentifier = localeTag.replace('-', '_'))
        dateTimeStyle = if (style.numeric == NumberStyle.RelativeTime.Numeric.ALWAYS) {
            NSRelativeDateTimeFormatterStyleNumeric
        } else {
            NSRelativeDateTimeFormatterStyleNamed
        }
        unitsStyle = when (style.width) {
            NumberStyle.RelativeTime.Width.LONG -> NSRelativeDateTimeFormatterUnitsStyleFull
            NumberStyle.RelativeTime.Width.SHORT -> NSRelativeDateTimeFormatterUnitsStyleShort
            NumberStyle.RelativeTime.Width.NARROW -> NSRelativeDateTimeFormatterUnitsStyleAbbreviated
        }
    }

    override fun format(value: DecimalInput): String {
        val n: Long = when (value) {
            is DecimalInput.OfDouble -> if (!value.value.isFinite()) return nonFinite(value.value) else value.value.toLong()
            is DecimalInput.OfLong -> value.value
            is DecimalInput.OfString -> value.value.toDouble().toLong()
        }
        val comps = NSDateComponents()
        when (style.unit) {
            TimeUnit.SECOND -> comps.second = n
            TimeUnit.MINUTE -> comps.minute = n
            TimeUnit.HOUR -> comps.hour = n
            TimeUnit.DAY -> comps.day = n
            TimeUnit.WEEK -> comps.weekOfYear = n
            TimeUnit.MONTH -> comps.month = n
            TimeUnit.YEAR -> comps.year = n
            TimeUnit.QUARTER -> return n.toString()
        }
        return fmt.localizedStringFromDateComponents(comps)
    }
}

private class SpelloutAppleFormatter(localeTag: String) : PlatformFormatter {
    private val nf = NSNumberFormatter().apply {
        locale = NSLocale(localeIdentifier = localeTag.replace('-', '_'))
        numberStyle = NSNumberFormatterSpellOutStyle
    }

    override fun format(value: DecimalInput): String {
        val n: Long = when (value) {
            is DecimalInput.OfDouble -> if (!value.value.isFinite()) return nonFinite(value.value) else value.value.toLong()
            is DecimalInput.OfLong -> value.value
            is DecimalInput.OfString -> value.value.toDouble().toLong()
        }
        return nf.stringFromNumber(NSNumber(long = n)) ?: n.toString()
    }
}

private class OrdinalAppleFormatter(localeTag: String) : PlatformFormatter {
    private val nf = NSNumberFormatter().apply {
        locale = NSLocale(localeIdentifier = localeTag.replace('-', '_'))
        numberStyle = NSNumberFormatterOrdinalStyle
    }

    override fun format(value: DecimalInput): String {
        val n: Long = when (value) {
            is DecimalInput.OfDouble -> if (!value.value.isFinite()) return nonFinite(value.value) else value.value.toLong()
            is DecimalInput.OfLong -> value.value
            is DecimalInput.OfString -> value.value.toDouble().toLong()
        }
        return nf.stringFromNumber(NSNumber(long = n)) ?: n.toString()
    }
}

private class CurrencyAppleFormatter(
    localeTag: String,
    private val style: NumberStyle.Currency,
) : PlatformFormatter {

    private val nf = NSNumberFormatter().apply {
        locale = NSLocale(localeIdentifier = localeTag.replace('-', '_'))
        numberStyle = when (style.presentation) {
            NumberStyle.Currency.Presentation.SYMBOL -> NSNumberFormatterCurrencyStyle
            NumberStyle.Currency.Presentation.ISO_CODE -> NSNumberFormatterCurrencyISOCodeStyle
            NumberStyle.Currency.Presentation.ACCOUNTING -> NSNumberFormatterCurrencyAccountingStyle
        }
        currencyCode = style.currencyCode
        usesGroupingSeparator = style.grouping
        roundingMode = style.rounding.toNs()
        style.minFractionDigits?.let { minimumFractionDigits = it.convert() }
        style.maxFractionDigits?.let { maximumFractionDigits = it.convert() }
    }
    private val minus: String = nf.minusSign ?: "-"

    override fun format(value: DecimalInput): String {
        val negative: Boolean
        val zero: Boolean
        val number: NSNumber
        when (value) {
            is DecimalInput.OfDouble -> {
                if (!value.value.isFinite()) return nonFinite(value.value)
                negative = value.value < 0.0
                zero = value.value == 0.0
                number = NSNumber(double = value.value)
            }
            is DecimalInput.OfLong -> {
                negative = value.value < 0L
                zero = value.value == 0L
                number = NSNumber(long = value.value)
            }
            is DecimalInput.OfString -> {
                val dn = NSDecimalNumber(string = value.value)
                val sign = dn.doubleValue
                negative = sign < 0.0
                zero = sign == 0.0
                number = dn
            }
        }
        val text = nf.stringFromNumber(number) ?: number.stringValue
        return applySign(text, negative, zero, style.signDisplay, minus)
    }
}

private class DecimalAppleFormatter(
    localeTag: String,
    private val style: NumberStyle.Decimal,
) : PlatformFormatter {

    private val nf = NSNumberFormatter().apply {
        locale = NSLocale(localeIdentifier = localeTag.replace('-', '_'))
        numberStyle = NSNumberFormatterDecimalStyle
        usesGroupingSeparator = style.grouping
        minimumFractionDigits = style.minFractionDigits.convert()
        maximumFractionDigits = style.maxFractionDigits.convert()
        roundingMode = style.rounding.toNs()
    }
    private val minus: String = nf.minusSign ?: "-"

    override fun format(value: DecimalInput): String {
        val negative: Boolean
        val zero: Boolean
        val number: NSNumber
        when (value) {
            is DecimalInput.OfDouble -> {
                if (!value.value.isFinite()) return nonFinite(value.value)
                negative = value.value < 0.0
                zero = value.value == 0.0
                number = NSNumber(double = value.value)
            }
            is DecimalInput.OfLong -> {
                negative = value.value < 0L
                zero = value.value == 0L
                number = NSNumber(long = value.value)
            }
            is DecimalInput.OfString -> {
                val dn = NSDecimalNumber(string = value.value)
                val sign = dn.doubleValue
                negative = sign < 0.0
                zero = sign == 0.0
                number = dn
            }
        }
        val text = nf.stringFromNumber(number) ?: number.stringValue
        return applySign(text, negative, zero, style.signDisplay, minus)
    }
}

private class PercentAppleFormatter(
    localeTag: String,
    private val style: NumberStyle.Percent,
) : PlatformFormatter {

    private val nf = NSNumberFormatter().apply {
        locale = NSLocale(localeIdentifier = localeTag.replace('-', '_'))
        numberStyle = NSNumberFormatterPercentStyle
        minimumFractionDigits = style.minFractionDigits.convert()
        maximumFractionDigits = style.maxFractionDigits.convert()
        roundingMode = style.rounding.toNs()
    }

    override fun format(value: DecimalInput): String {
        val ratio = ratioOf(value, style.scale)
        return if (!ratio.isFinite()) nonFinite(ratio) else (nf.stringFromNumber(NSNumber(double = ratio)) ?: ratio.toString())
    }
}

private class ScientificAppleFormatter(
    localeTag: String,
    style: NumberStyle.Scientific,
) : PlatformFormatter {

    private val nf = NSNumberFormatter().apply {
        locale = NSLocale(localeIdentifier = localeTag.replace('-', '_'))
        numberStyle = NSNumberFormatterScientificStyle
        maximumFractionDigits = style.maxFractionDigits.convert()
    }

    override fun format(value: DecimalInput): String {
        val number: NSNumber = when (value) {
            is DecimalInput.OfDouble -> if (!value.value.isFinite()) return nonFinite(value.value) else NSNumber(double = value.value)
            is DecimalInput.OfLong -> NSNumber(long = value.value)
            is DecimalInput.OfString -> NSDecimalNumber(string = value.value)
        }
        return nf.stringFromNumber(number) ?: number.stringValue
    }
}

private class CompactAppleFormatter(
    style: NumberStyle.Compact,
) : PlatformFormatter {

    private val maxFraction = style.maxFractionDigits
    private val nf = NSNumberFormatter().apply {
        locale = NSLocale(localeIdentifier = "en_US")
        numberStyle = NSNumberFormatterDecimalStyle
        usesGroupingSeparator = false
        minimumFractionDigits = 0uL.convert()
        maximumFractionDigits = maxFraction.convert()
        roundingMode = NSNumberFormatterRoundHalfEven
    }

    override fun format(value: DecimalInput): String {
        val d = when (value) {
            is DecimalInput.OfDouble -> if (!value.value.isFinite()) return nonFinite(value.value) else value.value
            is DecimalInput.OfLong -> value.value.toDouble()
            is DecimalInput.OfString -> value.value.toDouble()
        }
        val magnitude = abs(d)
        if (magnitude < 1e3) return nf.stringFromNumber(NSNumber(double = d)) ?: d.toString()

        var scale = when {
            magnitude >= 1e12 -> 1e12
            magnitude >= 1e9 -> 1e9
            magnitude >= 1e6 -> 1e6
            else -> 1e3
        }
        if (scale < 1e12 && roundToDigits(magnitude / scale, maxFraction) >= 1000.0) {
            scale *= 1e3
        }
        val head = nf.stringFromNumber(NSNumber(double = d / scale)) ?: (d / scale).toString()
        return head + suffixFor(scale)
    }

    private fun roundToDigits(x: Double, digits: Int): Double {
        val factor = 10.0.pow(digits)
        return round(x * factor) / factor
    }

    private fun suffixFor(scale: Double): String = when (scale) {
        1e12 -> "T"
        1e9 -> "B"
        1e6 -> "M"
        else -> "K"
    }
}

private fun ratioOf(value: DecimalInput, scale: NumberStyle.Percent.Scale): Double {
    val base = when (value) {
        is DecimalInput.OfDouble -> value.value
        is DecimalInput.OfLong -> value.value.toDouble()
        is DecimalInput.OfString -> value.value.toDouble()
    }
    return if (scale == NumberStyle.Percent.Scale.VALUE) base / 100.0 else base
}

private fun applySign(
    text: String,
    negative: Boolean,
    zero: Boolean,
    signDisplay: SignDisplay,
    minus: String,
): String = when (signDisplay) {
    SignDisplay.AUTO -> text
    SignDisplay.NEVER -> if (negative) text.replaceFirst(minus, "").removeSurrounding("(", ")") else text
    SignDisplay.ALWAYS -> if (!negative) "+$text" else text
    SignDisplay.EXCEPT_ZERO -> if (!negative && !zero) "+$text" else text
}

private fun looksLikeLanguageTag(tag: String): Boolean {
    val lang = tag.substringBefore('-').substringBefore('_')
    return lang.length in 2..3 && lang.all { it in 'a'..'z' || it in 'A'..'Z' }
}

private fun nonFinite(value: Double): String = when {
    value.isNaN() -> "NaN"
    value > 0 -> "∞"
    else -> "-∞"
}

private fun RoundingMode.toNs(): NSNumberFormatterRoundingMode = when (this) {
    RoundingMode.HALF_UP -> NSNumberFormatterRoundHalfUp
    RoundingMode.HALF_EVEN -> NSNumberFormatterRoundHalfEven
    RoundingMode.HALF_DOWN -> NSNumberFormatterRoundHalfDown
    RoundingMode.UP -> NSNumberFormatterRoundUp
    RoundingMode.DOWN -> NSNumberFormatterRoundDown
    RoundingMode.CEILING -> NSNumberFormatterRoundCeiling
    RoundingMode.FLOOR -> NSNumberFormatterRoundFloor
}
