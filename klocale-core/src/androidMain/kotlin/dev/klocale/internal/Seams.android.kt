package dev.klocale.internal

import android.icu.math.BigDecimal as IcuRounding
import android.icu.text.CompactDecimalFormat
import android.icu.text.DecimalFormat
import android.icu.text.DecimalFormatSymbols
import android.icu.text.MeasureFormat
import android.icu.text.NumberFormat
import android.icu.text.PluralRules
import android.icu.text.RelativeDateTimeFormatter
import android.icu.util.Currency
import android.icu.util.Measure
import android.icu.util.MeasureUnit as IcuMeasureUnit
import android.icu.util.ULocale
import dev.klocale.MeasureUnit
import dev.klocale.NumberFormatError
import dev.klocale.NumberStyle
import dev.klocale.RoundingMode
import dev.klocale.SignDisplay
import dev.klocale.TimeUnit
import kotlin.math.abs
import java.math.BigDecimal
import java.util.Locale

internal actual val backendName: String = "android.icu"

internal actual fun currentLocaleTag(): String = Locale.getDefault().toLanguageTag()

internal actual fun platformLocaleInfo(languageTag: String): LocaleInfo? {
    val uloc = ULocale.forLanguageTag(languageTag)
    if (uloc.language.isNullOrEmpty()) return null
    val symbols = DecimalFormatSymbols.getInstance(uloc)
    return LocaleInfo(
        canonicalTag = uloc.toLanguageTag(),
        decimalSeparator = symbols.decimalSeparator,
        groupingSeparator = symbols.groupingSeparator,
    )
}

internal actual fun createPlatformFormatter(spec: FormatSpec): PlatformFormatter {
    val uloc = ULocale.forLanguageTag(spec.localeTag)
    return when (val style = spec.style) {
        is NumberStyle.Decimal -> DecimalAndroidFormatter(uloc, style)
        is NumberStyle.Currency -> CurrencyAndroidFormatter(uloc, style)
        is NumberStyle.Percent -> PercentAndroidFormatter(uloc, style)
        is NumberStyle.Scientific -> ScientificAndroidFormatter(uloc, style)
        is NumberStyle.Compact -> CompactAndroidFormatter(uloc, style)
        is NumberStyle.Ordinal -> {
            val language = spec.localeTag.substringBefore('-').lowercase()
            if (style.kind == NumberStyle.Ordinal.Kind.SUFFIX && language == "en") {
                OrdinalAndroidFormatter(uloc)
            } else {
                throw NumberFormatError.UnsupportedStyle(style, backendName)
            }
        }
        is NumberStyle.RelativeTime ->
            if (style.unit == TimeUnit.QUARTER) throw NumberFormatError.UnsupportedStyle(style, backendName) else RelativeTimeAndroidFormatter(uloc, style)
        is NumberStyle.Measure -> MeasureAndroidFormatter(uloc, style)
        else -> throw NumberFormatError.UnsupportedStyle(style, backendName)
    }
}

private class CurrencyAndroidFormatter(
    uloc: ULocale,
    private val style: NumberStyle.Currency,
) : PlatformFormatter {

    private val df: DecimalFormat = run {
        val icuStyle = when (style.presentation) {
            NumberStyle.Currency.Presentation.SYMBOL -> NumberFormat.CURRENCYSTYLE
            NumberStyle.Currency.Presentation.ISO_CODE -> NumberFormat.ISOCURRENCYSTYLE
            NumberStyle.Currency.Presentation.ACCOUNTING -> NumberFormat.ACCOUNTINGCURRENCYSTYLE
        }
        (NumberFormat.getInstance(uloc, icuStyle) as DecimalFormat).apply {
            currency = Currency.getInstance(style.currencyCode)
            isGroupingUsed = style.grouping
            roundingMode = style.rounding.toIcu()
            style.minFractionDigits?.let { minimumFractionDigits = it }
            style.maxFractionDigits?.let { maximumFractionDigits = it }
        }
    }
    private val minusSign: String = df.decimalFormatSymbols.minusSign.toString()

    override fun format(value: DecimalInput): String {
        val negative: Boolean
        val zero: Boolean
        val text: String
        when (value) {
            is DecimalInput.OfDouble -> {
                if (!value.value.isFinite()) return nonFinite(value.value)
                negative = value.value < 0.0
                zero = value.value == 0.0
                text = df.format(value.value)
            }
            is DecimalInput.OfLong -> {
                negative = value.value < 0L
                zero = value.value == 0L
                text = df.format(value.value)
            }
            is DecimalInput.OfString -> {
                val bd = BigDecimal(value.value)
                negative = bd.signum() < 0
                zero = bd.signum() == 0
                text = df.format(bd as Any)
            }
        }
        return applySign(text, negative, zero, style.signDisplay, minusSign)
    }
}

private class DecimalAndroidFormatter(
    uloc: ULocale,
    private val style: NumberStyle.Decimal,
) : PlatformFormatter {

    private val df: DecimalFormat = (NumberFormat.getInstance(uloc) as DecimalFormat).apply {
        isGroupingUsed = style.grouping
        minimumFractionDigits = style.minFractionDigits
        maximumFractionDigits = style.maxFractionDigits
        roundingMode = style.rounding.toIcu()
    }
    private val minusSign: String = df.decimalFormatSymbols.minusSign.toString()

    override fun format(value: DecimalInput): String {
        val negative: Boolean
        val zero: Boolean
        val text: String
        when (value) {
            is DecimalInput.OfDouble -> {
                if (!value.value.isFinite()) return nonFinite(value.value)
                negative = value.value < 0.0
                zero = value.value == 0.0
                text = df.format(value.value)
            }
            is DecimalInput.OfLong -> {
                negative = value.value < 0L
                zero = value.value == 0L
                text = df.format(value.value)
            }
            is DecimalInput.OfString -> {
                val bd = BigDecimal(value.value)
                negative = bd.signum() < 0
                zero = bd.signum() == 0
                text = df.format(bd as Any)
            }
        }
        return applySign(text, negative, zero, style.signDisplay, minusSign)
    }
}

private class PercentAndroidFormatter(
    uloc: ULocale,
    private val style: NumberStyle.Percent,
) : PlatformFormatter {

    private val df: DecimalFormat = (NumberFormat.getInstance(uloc, NumberFormat.PERCENTSTYLE) as DecimalFormat).apply {
        minimumFractionDigits = style.minFractionDigits
        maximumFractionDigits = style.maxFractionDigits
        roundingMode = style.rounding.toIcu()
    }

    override fun format(value: DecimalInput): String {
        val ratio = ratioOf(value, style.scale)
        return if (!ratio.isFinite()) nonFinite(ratio) else df.format(ratio)
    }
}

private class ScientificAndroidFormatter(
    uloc: ULocale,
    style: NumberStyle.Scientific,
) : PlatformFormatter {

    private val df: DecimalFormat = (NumberFormat.getInstance(uloc, NumberFormat.SCIENTIFICSTYLE) as DecimalFormat).apply {
        maximumFractionDigits = style.maxFractionDigits
        if (style.engineering) {
            minimumIntegerDigits = 1
            maximumIntegerDigits = 3
        }
    }

    override fun format(value: DecimalInput): String = when (value) {
        is DecimalInput.OfDouble -> if (!value.value.isFinite()) nonFinite(value.value) else df.format(value.value)
        is DecimalInput.OfLong -> df.format(value.value)
        is DecimalInput.OfString -> df.format(BigDecimal(value.value) as Any)
    }
}

private class CompactAndroidFormatter(
    uloc: ULocale,
    style: NumberStyle.Compact,
) : PlatformFormatter {

    private val cdf: CompactDecimalFormat = CompactDecimalFormat.getInstance(
        uloc,
        if (style.length == NumberStyle.Compact.Length.LONG) {
            CompactDecimalFormat.CompactStyle.LONG
        } else {
            CompactDecimalFormat.CompactStyle.SHORT
        },
    ).apply {
        maximumFractionDigits = style.maxFractionDigits
    }

    override fun format(value: DecimalInput): String = when (value) {
        is DecimalInput.OfDouble -> if (!value.value.isFinite()) nonFinite(value.value) else cdf.format(value.value)
        is DecimalInput.OfLong -> cdf.format(value.value)
        is DecimalInput.OfString -> cdf.format(value.value.toDouble())
    }
}

private class OrdinalAndroidFormatter(uloc: ULocale) : PlatformFormatter {
    private val rules = PluralRules.forLocale(uloc, PluralRules.PluralType.ORDINAL)

    override fun format(value: DecimalInput): String {
        val n = toLongValue(value) ?: return nonFinite((value as DecimalInput.OfDouble).value)
        val suffix = when (rules.select(n.toDouble())) {
            "one" -> "st"
            "two" -> "nd"
            "few" -> "rd"
            else -> "th"
        }
        return "$n$suffix"
    }
}

private class MeasureAndroidFormatter(
    uloc: ULocale,
    style: NumberStyle.Measure,
) : PlatformFormatter {

    private val unit = toIcuMeasureUnit(style.unit)
    private val mf: MeasureFormat = run {
        val numbers = (NumberFormat.getInstance(uloc) as DecimalFormat).apply {
            maximumFractionDigits = style.maxFractionDigits
        }
        val width = when (style.width) {
            NumberStyle.Measure.Width.NARROW -> MeasureFormat.FormatWidth.NARROW
            NumberStyle.Measure.Width.SHORT -> MeasureFormat.FormatWidth.SHORT
            NumberStyle.Measure.Width.LONG -> MeasureFormat.FormatWidth.WIDE
        }
        MeasureFormat.getInstance(uloc, width, numbers)
    }

    override fun format(value: DecimalInput): String {
        val v = when (value) {
            is DecimalInput.OfDouble -> if (!value.value.isFinite()) return nonFinite(value.value) else value.value
            is DecimalInput.OfLong -> value.value.toDouble()
            is DecimalInput.OfString -> value.value.toDouble()
        }
        return mf.format(Measure(v, unit))
    }
}

private fun toIcuMeasureUnit(unit: MeasureUnit): IcuMeasureUnit = when (unit) {
    MeasureUnit.METER -> IcuMeasureUnit.METER
    MeasureUnit.KILOMETER -> IcuMeasureUnit.KILOMETER
    MeasureUnit.CENTIMETER -> IcuMeasureUnit.CENTIMETER
    MeasureUnit.MILE -> IcuMeasureUnit.MILE
    MeasureUnit.FOOT -> IcuMeasureUnit.FOOT
    MeasureUnit.GRAM -> IcuMeasureUnit.GRAM
    MeasureUnit.KILOGRAM -> IcuMeasureUnit.KILOGRAM
    MeasureUnit.POUND -> IcuMeasureUnit.POUND
    MeasureUnit.OUNCE -> IcuMeasureUnit.OUNCE
    MeasureUnit.LITER -> IcuMeasureUnit.LITER
    MeasureUnit.MILLILITER -> IcuMeasureUnit.MILLILITER
    MeasureUnit.CELSIUS -> IcuMeasureUnit.CELSIUS
    MeasureUnit.FAHRENHEIT -> IcuMeasureUnit.FAHRENHEIT
    MeasureUnit.BYTE -> IcuMeasureUnit.BYTE
    MeasureUnit.KILOBYTE -> IcuMeasureUnit.KILOBYTE
    MeasureUnit.MEGABYTE -> IcuMeasureUnit.MEGABYTE
    MeasureUnit.GIGABYTE -> IcuMeasureUnit.GIGABYTE
    MeasureUnit.SECOND -> IcuMeasureUnit.SECOND
    MeasureUnit.MINUTE -> IcuMeasureUnit.MINUTE
    MeasureUnit.HOUR -> IcuMeasureUnit.HOUR
    MeasureUnit.DAY -> IcuMeasureUnit.DAY
}

private class RelativeTimeAndroidFormatter(
    uloc: ULocale,
    style: NumberStyle.RelativeTime,
) : PlatformFormatter {

    private val fmt = RelativeDateTimeFormatter.getInstance(uloc)
    private val unit = toRelativeUnit(style.unit)

    override fun format(value: DecimalInput): String {
        val q = when (value) {
            is DecimalInput.OfDouble -> if (!value.value.isFinite()) return nonFinite(value.value) else value.value
            is DecimalInput.OfLong -> value.value.toDouble()
            is DecimalInput.OfString -> value.value.toDouble()
        }
        val direction = if (q >= 0) RelativeDateTimeFormatter.Direction.NEXT else RelativeDateTimeFormatter.Direction.LAST
        return fmt.format(abs(q), direction, unit)
    }
}

private fun toRelativeUnit(unit: TimeUnit): RelativeDateTimeFormatter.RelativeUnit = when (unit) {
    TimeUnit.SECOND -> RelativeDateTimeFormatter.RelativeUnit.SECONDS
    TimeUnit.MINUTE -> RelativeDateTimeFormatter.RelativeUnit.MINUTES
    TimeUnit.HOUR -> RelativeDateTimeFormatter.RelativeUnit.HOURS
    TimeUnit.DAY -> RelativeDateTimeFormatter.RelativeUnit.DAYS
    TimeUnit.WEEK -> RelativeDateTimeFormatter.RelativeUnit.WEEKS
    TimeUnit.MONTH -> RelativeDateTimeFormatter.RelativeUnit.MONTHS
    TimeUnit.QUARTER -> error("quarter is guarded before construction")
    TimeUnit.YEAR -> RelativeDateTimeFormatter.RelativeUnit.YEARS
}

private fun toLongValue(value: DecimalInput): Long? = when (value) {
    is DecimalInput.OfDouble -> if (value.value.isFinite()) value.value.toLong() else null
    is DecimalInput.OfLong -> value.value
    is DecimalInput.OfString -> value.value.toDouble().toLong()
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
    minusSign: String,
): String = when (signDisplay) {
    SignDisplay.AUTO -> text
    SignDisplay.NEVER -> if (negative) text.replaceFirst(minusSign, "").removeSurrounding("(", ")") else text
    SignDisplay.ALWAYS -> if (!negative) "+$text" else text
    SignDisplay.EXCEPT_ZERO -> if (!negative && !zero) "+$text" else text
}

private fun nonFinite(value: Double): String = when {
    value.isNaN() -> "NaN"
    value > 0 -> "∞"
    else -> "-∞"
}

private fun RoundingMode.toIcu(): Int = when (this) {
    RoundingMode.HALF_UP -> IcuRounding.ROUND_HALF_UP
    RoundingMode.HALF_EVEN -> IcuRounding.ROUND_HALF_EVEN
    RoundingMode.HALF_DOWN -> IcuRounding.ROUND_HALF_DOWN
    RoundingMode.UP -> IcuRounding.ROUND_UP
    RoundingMode.DOWN -> IcuRounding.ROUND_DOWN
    RoundingMode.CEILING -> IcuRounding.ROUND_CEILING
    RoundingMode.FLOOR -> IcuRounding.ROUND_FLOOR
}
