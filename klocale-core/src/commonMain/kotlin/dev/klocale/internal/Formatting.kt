package dev.klocale.internal

import dev.klocale.NumberStyle
import dev.klocale.SignDisplay

internal fun nonFinite(value: Double): String =
    when {
        value.isNaN() -> "NaN"
        value > 0 -> "∞"
        else -> "-∞"
    }

internal fun toLongValue(value: DecimalInput): Long? =
    when (value) {
        is DecimalInput.OfDouble -> if (value.value.isFinite()) value.value.toLong() else null
        is DecimalInput.OfLong -> value.value
        is DecimalInput.OfString -> value.value.toDouble().toLong()
    }

internal fun ratioOf(
    value: DecimalInput,
    scale: NumberStyle.Percent.Scale,
): Double {
    val base =
        when (value) {
            is DecimalInput.OfDouble -> value.value
            is DecimalInput.OfLong -> value.value.toDouble()
            is DecimalInput.OfString -> value.value.toDouble()
        }
    return if (scale == NumberStyle.Percent.Scale.VALUE) base / 100.0 else base
}

internal fun applySign(
    text: String,
    negative: Boolean,
    zero: Boolean,
    signDisplay: SignDisplay,
    minusSign: String,
): String =
    when (signDisplay) {
        SignDisplay.AUTO -> text
        SignDisplay.NEVER -> if (negative) text.replaceFirst(minusSign, "").removeSurrounding("(", ")") else text
        SignDisplay.ALWAYS -> if (!negative) "+$text" else text
        SignDisplay.EXCEPT_ZERO -> if (!negative && !zero) "+$text" else text
    }
