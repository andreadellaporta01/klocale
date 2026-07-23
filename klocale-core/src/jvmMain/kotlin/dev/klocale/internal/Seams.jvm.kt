package dev.klocale.internal

import com.ibm.icu.math.BigDecimal as IcuRounding
import com.ibm.icu.text.CompactDecimalFormat
import com.ibm.icu.text.DecimalFormat
import com.ibm.icu.text.DecimalFormatSymbols
import com.ibm.icu.text.NumberFormat
import com.ibm.icu.text.RelativeDateTimeFormatter
import com.ibm.icu.text.RuleBasedNumberFormat
import com.ibm.icu.util.Currency
import com.ibm.icu.util.ULocale
import dev.klocale.NumberFormatError
import dev.klocale.NumberStyle
import dev.klocale.RoundingMode
import dev.klocale.SignDisplay
import dev.klocale.TimeUnit
import kotlin.math.abs
import java.math.BigDecimal
import java.util.Locale

internal actual val backendName: String = "icu4j"

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
        is NumberStyle.Decimal -> DecimalJvmFormatter(uloc, style)
        is NumberStyle.Currency -> CurrencyJvmFormatter(uloc, style)
        is NumberStyle.Percent -> PercentJvmFormatter(uloc, style)
        is NumberStyle.Scientific -> ScientificJvmFormatter(uloc, style)
        is NumberStyle.Compact -> CompactJvmFormatter(uloc, style)
        is NumberStyle.Ordinal ->
            if (style.kind == NumberStyle.Ordinal.Kind.SUFFIX) OrdinalJvmFormatter(uloc) else throw NumberFormatError.UnsupportedStyle(style, backendName)
        is NumberStyle.Spellout -> SpelloutJvmFormatter(uloc)
        is NumberStyle.RelativeTime -> RelativeTimeJvmFormatter(uloc, style)
        else -> throw NumberFormatError.UnsupportedStyle(style, backendName)
    }
}

private class CurrencyJvmFormatter(
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

private class DecimalJvmFormatter(
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

private class PercentJvmFormatter(
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

private class ScientificJvmFormatter(
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

private class CompactJvmFormatter(
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

private class OrdinalJvmFormatter(uloc: ULocale) : PlatformFormatter {
    private val rbnf = RuleBasedNumberFormat(uloc, RuleBasedNumberFormat.ORDINAL)

    override fun format(value: DecimalInput): String {
        val n = toLongValue(value) ?: return nonFinite((value as DecimalInput.OfDouble).value)
        return rbnf.format(n)
    }
}

private class RelativeTimeJvmFormatter(
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
    TimeUnit.QUARTER -> RelativeDateTimeFormatter.RelativeUnit.QUARTERS
    TimeUnit.YEAR -> RelativeDateTimeFormatter.RelativeUnit.YEARS
}

private class SpelloutJvmFormatter(uloc: ULocale) : PlatformFormatter {
    private val rbnf = RuleBasedNumberFormat(uloc, RuleBasedNumberFormat.SPELLOUT)

    override fun format(value: DecimalInput): String {
        val n = toLongValue(value) ?: return nonFinite((value as DecimalInput.OfDouble).value)
        return rbnf.format(n)
    }
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
    SignDisplay.NEVER -> if (negative) text.replaceFirst(minusSign, "") else text
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
