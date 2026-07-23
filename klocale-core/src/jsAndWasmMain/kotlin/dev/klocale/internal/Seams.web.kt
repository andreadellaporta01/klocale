package dev.klocale.internal

import dev.klocale.MeasureUnit
import dev.klocale.NumberFormatError
import dev.klocale.NumberStyle
import dev.klocale.RoundingMode
import dev.klocale.SignDisplay

internal actual val backendName: String = "Intl"

internal actual fun currentLocaleTag(): String {
    val tag = resolvedLocale("")
    return tag.ifEmpty { "en-US" }
}

internal actual fun platformLocaleInfo(languageTag: String): LocaleInfo? {
    val canonical = resolvedLocale(languageTag)
    if (canonical.isEmpty()) return null
    val seps = localeSeparators(languageTag).split('|')
    val group = seps.getOrNull(0)?.firstOrNull() ?: ','
    val decimal = seps.getOrNull(1)?.firstOrNull() ?: '.'
    return LocaleInfo(canonicalTag = canonical, decimalSeparator = decimal, groupingSeparator = group)
}

internal actual fun createPlatformFormatter(spec: FormatSpec): PlatformFormatter {
    return when (val style = spec.style) {
        is NumberStyle.Decimal -> DecimalWebFormatter(spec.localeTag, style)
        is NumberStyle.Currency -> CurrencyWebFormatter(spec.localeTag, style)
        is NumberStyle.Percent -> PercentWebFormatter(spec.localeTag, style)
        is NumberStyle.Scientific -> ScientificWebFormatter(spec.localeTag, style)
        is NumberStyle.Compact -> CompactWebFormatter(spec.localeTag, style)
        is NumberStyle.Ordinal -> {
            val language = spec.localeTag.substringBefore('-').lowercase()
            if (style.kind == NumberStyle.Ordinal.Kind.SUFFIX && language == "en") {
                OrdinalWebFormatter(spec.localeTag)
            } else {
                throw NumberFormatError.UnsupportedStyle(style, backendName)
            }
        }
        is NumberStyle.RelativeTime -> RelativeTimeWebFormatter(spec.localeTag, style)
        is NumberStyle.Measure -> MeasureWebFormatter(spec.localeTag, style)
        else -> throw NumberFormatError.UnsupportedStyle(style, backendName)
    }
}

private class CurrencyWebFormatter(
    private val localeTag: String,
    private val style: NumberStyle.Currency,
) : PlatformFormatter {

    private val options: String = buildString {
        append("{\"style\":\"currency\"")
        append(",\"currency\":\"").append(style.currencyCode).append('"')
        val display = when (style.presentation) {
            NumberStyle.Currency.Presentation.ISO_CODE -> "code"
            else -> "symbol"
        }
        append(",\"currencyDisplay\":\"").append(display).append('"')
        if (style.presentation == NumberStyle.Currency.Presentation.ACCOUNTING) {
            append(",\"currencySign\":\"accounting\"")
        }
        append(",\"useGrouping\":").append(style.grouping)
        append(",\"roundingMode\":\"").append(style.rounding.toIntl()).append('"')
        append(",\"signDisplay\":\"").append(style.signDisplay.toIntl()).append('"')
        style.minFractionDigits?.let { append(",\"minimumFractionDigits\":").append(it) }
        style.maxFractionDigits?.let { append(",\"maximumFractionDigits\":").append(it) }
        append('}')
    }

    override fun format(value: DecimalInput): String = when (value) {
        is DecimalInput.OfDouble ->
            if (!value.value.isFinite()) nonFinite(value.value) else intlFormat(localeTag, options, value.value)
        is DecimalInput.OfLong -> intlFormat(localeTag, options, value.value.toDouble())
        is DecimalInput.OfString -> intlFormatString(localeTag, options, value.value)
    }
}

private class DecimalWebFormatter(
    private val localeTag: String,
    private val style: NumberStyle.Decimal,
) : PlatformFormatter {

    private val options: String = buildString {
        append("{\"style\":\"decimal\"")
        append(",\"useGrouping\":").append(style.grouping)
        append(",\"minimumFractionDigits\":").append(style.minFractionDigits)
        append(",\"maximumFractionDigits\":").append(style.maxFractionDigits)
        append(",\"roundingMode\":\"").append(style.rounding.toIntl()).append('"')
        append(",\"signDisplay\":\"").append(style.signDisplay.toIntl()).append('"')
        append('}')
    }

    override fun format(value: DecimalInput): String = when (value) {
        is DecimalInput.OfDouble ->
            if (!value.value.isFinite()) nonFinite(value.value) else intlFormat(localeTag, options, value.value)
        is DecimalInput.OfLong -> intlFormat(localeTag, options, value.value.toDouble())
        is DecimalInput.OfString -> intlFormatString(localeTag, options, value.value)
    }
}

private class PercentWebFormatter(
    private val localeTag: String,
    private val style: NumberStyle.Percent,
) : PlatformFormatter {

    private val options: String = buildString {
        append("{\"style\":\"percent\"")
        append(",\"minimumFractionDigits\":").append(style.minFractionDigits)
        append(",\"maximumFractionDigits\":").append(style.maxFractionDigits)
        append(",\"roundingMode\":\"").append(style.rounding.toIntl()).append('"')
        append('}')
    }

    override fun format(value: DecimalInput): String {
        val ratio = ratioOf(value, style.scale)
        return if (!ratio.isFinite()) nonFinite(ratio) else intlFormat(localeTag, options, ratio)
    }
}

private class ScientificWebFormatter(
    private val localeTag: String,
    style: NumberStyle.Scientific,
) : PlatformFormatter {

    private val options: String = buildString {
        append("{\"notation\":\"").append(if (style.engineering) "engineering" else "scientific").append('"')
        append(",\"maximumFractionDigits\":").append(style.maxFractionDigits)
        append('}')
    }

    override fun format(value: DecimalInput): String = when (value) {
        is DecimalInput.OfDouble -> if (!value.value.isFinite()) nonFinite(value.value) else intlFormat(localeTag, options, value.value)
        is DecimalInput.OfLong -> intlFormat(localeTag, options, value.value.toDouble())
        is DecimalInput.OfString -> intlFormatString(localeTag, options, value.value)
    }
}

private class CompactWebFormatter(
    private val localeTag: String,
    style: NumberStyle.Compact,
) : PlatformFormatter {

    private val display = if (style.length == NumberStyle.Compact.Length.LONG) "long" else "short"
    private val options: String = buildString {
        append("{\"notation\":\"compact\"")
        append(",\"compactDisplay\":\"").append(display).append('"')
        append(",\"maximumFractionDigits\":").append(style.maxFractionDigits)
        append('}')
    }

    override fun format(value: DecimalInput): String = when (value) {
        is DecimalInput.OfDouble -> if (!value.value.isFinite()) nonFinite(value.value) else intlFormat(localeTag, options, value.value)
        is DecimalInput.OfLong -> intlFormat(localeTag, options, value.value.toDouble())
        is DecimalInput.OfString -> intlFormatString(localeTag, options, value.value)
    }
}

private class OrdinalWebFormatter(private val localeTag: String) : PlatformFormatter {
    override fun format(value: DecimalInput): String {
        val n: Long = when (value) {
            is DecimalInput.OfDouble -> if (!value.value.isFinite()) return nonFinite(value.value) else value.value.toLong()
            is DecimalInput.OfLong -> value.value
            is DecimalInput.OfString -> value.value.toDouble().toLong()
        }
        val suffix = when (ordinalCategory(localeTag, n.toDouble())) {
            "one" -> "st"
            "two" -> "nd"
            "few" -> "rd"
            else -> "th"
        }
        return "$n$suffix"
    }
}

private fun ordinalCategory(locale: String, n: Double): String =
    js("new Intl.PluralRules(locale, { type: 'ordinal' }).select(n)")

private class RelativeTimeWebFormatter(
    private val localeTag: String,
    style: NumberStyle.RelativeTime,
) : PlatformFormatter {

    private val numeric = if (style.numeric == NumberStyle.RelativeTime.Numeric.ALWAYS) "always" else "auto"
    private val width = style.width.name.lowercase()
    private val unit = style.unit.name.lowercase()

    override fun format(value: DecimalInput): String {
        val v = when (value) {
            is DecimalInput.OfDouble -> if (!value.value.isFinite()) return nonFinite(value.value) else value.value
            is DecimalInput.OfLong -> value.value.toDouble()
            is DecimalInput.OfString -> value.value.toDouble()
        }
        return intlRelative(localeTag, numeric, width, unit, v)
    }
}

private fun intlRelative(locale: String, numeric: String, style: String, unit: String, value: Double): String =
    js("new Intl.RelativeTimeFormat(locale, { numeric: numeric, style: style }).format(value, unit)")

private class MeasureWebFormatter(
    private val localeTag: String,
    style: NumberStyle.Measure,
) : PlatformFormatter {

    private val display = when (style.width) {
        NumberStyle.Measure.Width.NARROW -> "narrow"
        NumberStyle.Measure.Width.SHORT -> "short"
        NumberStyle.Measure.Width.LONG -> "long"
    }
    private val options: String = buildString {
        append("{\"style\":\"unit\"")
        append(",\"unit\":\"").append(intlUnit(style.unit)).append('"')
        append(",\"unitDisplay\":\"").append(display).append('"')
        append(",\"maximumFractionDigits\":").append(style.maxFractionDigits)
        append('}')
    }

    override fun format(value: DecimalInput): String = when (value) {
        is DecimalInput.OfDouble -> if (!value.value.isFinite()) nonFinite(value.value) else intlFormat(localeTag, options, value.value)
        is DecimalInput.OfLong -> intlFormat(localeTag, options, value.value.toDouble())
        is DecimalInput.OfString -> intlFormatString(localeTag, options, value.value)
    }
}

private fun intlUnit(unit: MeasureUnit): String = when (unit) {
    MeasureUnit.METER -> "meter"
    MeasureUnit.KILOMETER -> "kilometer"
    MeasureUnit.CENTIMETER -> "centimeter"
    MeasureUnit.MILE -> "mile"
    MeasureUnit.FOOT -> "foot"
    MeasureUnit.GRAM -> "gram"
    MeasureUnit.KILOGRAM -> "kilogram"
    MeasureUnit.POUND -> "pound"
    MeasureUnit.OUNCE -> "ounce"
    MeasureUnit.LITER -> "liter"
    MeasureUnit.MILLILITER -> "milliliter"
    MeasureUnit.CELSIUS -> "celsius"
    MeasureUnit.FAHRENHEIT -> "fahrenheit"
    MeasureUnit.BYTE -> "byte"
    MeasureUnit.KILOBYTE -> "kilobyte"
    MeasureUnit.MEGABYTE -> "megabyte"
    MeasureUnit.GIGABYTE -> "gigabyte"
    MeasureUnit.SECOND -> "second"
    MeasureUnit.MINUTE -> "minute"
    MeasureUnit.HOUR -> "hour"
    MeasureUnit.DAY -> "day"
}

private fun ratioOf(value: DecimalInput, scale: NumberStyle.Percent.Scale): Double {
    val base = when (value) {
        is DecimalInput.OfDouble -> value.value
        is DecimalInput.OfLong -> value.value.toDouble()
        is DecimalInput.OfString -> value.value.toDouble()
    }
    return if (scale == NumberStyle.Percent.Scale.VALUE) base / 100.0 else base
}

private fun SignDisplay.toIntl(): String = when (this) {
    SignDisplay.AUTO -> "auto"
    SignDisplay.ALWAYS -> "always"
    SignDisplay.NEVER -> "never"
    SignDisplay.EXCEPT_ZERO -> "exceptZero"
}

private fun RoundingMode.toIntl(): String = when (this) {
    RoundingMode.HALF_UP -> "halfExpand"
    RoundingMode.HALF_EVEN -> "halfEven"
    RoundingMode.HALF_DOWN -> "halfTrunc"
    RoundingMode.UP -> "expand"
    RoundingMode.DOWN -> "trunc"
    RoundingMode.CEILING -> "ceil"
    RoundingMode.FLOOR -> "floor"
}

private fun nonFinite(value: Double): String = when {
    value.isNaN() -> "NaN"
    value > 0 -> "∞"
    else -> "-∞"
}

private fun intlFormat(locale: String, optionsJson: String, value: Double): String =
    js("new Intl.NumberFormat(locale, JSON.parse(optionsJson)).format(value)")

private fun intlFormatString(locale: String, optionsJson: String, value: String): String =
    js("new Intl.NumberFormat(locale, JSON.parse(optionsJson)).format(value)")

private fun resolvedLocale(locale: String): String =
    js("(function(){try{return new Intl.NumberFormat(locale===''?undefined:locale).resolvedOptions().locale;}catch(e){return '';}})()")

private fun localeSeparators(locale: String): String =
    js("(function(){var p=new Intl.NumberFormat(locale).formatToParts(11111.1);var g='',d='';for(var i=0;i<p.length;i++){if(p[i].type==='group')g=p[i].value;if(p[i].type==='decimal')d=p[i].value;}return g+'|'+d;})()")
