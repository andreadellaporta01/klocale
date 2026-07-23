package dev.klocale

/** Rounding strategy applied when a value has more fraction digits than allowed. */
public enum class RoundingMode {
    HALF_UP,
    HALF_EVEN,
    HALF_DOWN,
    UP,
    DOWN,
    CEILING,
    FLOOR,
}

/** Controls when a sign is shown: only for negatives, always, never, or always except zero. */
public enum class SignDisplay {
    AUTO,
    ALWAYS,
    NEVER,
    EXCEPT_ZERO,
}

/**
 * The cross-platform consistency contract: normalizes cosmetic differences between the
 * native backends (`NSNumberFormatter`, `java.text`, `android.icu`, `Intl.NumberFormat`)
 * so that identical input yields identical output on every target.
 */
public data class NormalizationPolicy(
    /** Replace Unicode minus (U+2212) and locale minus glyphs with ASCII `'-'`. */
    val useAsciiMinus: Boolean = true,
    /** Space glyph used between grouped digits. */
    val groupingSpace: SpaceStyle = SpaceStyle.NON_BREAKING,
    /** Space glyph used between amount and currency symbol/code. */
    val currencySpace: SpaceStyle = SpaceStyle.NON_BREAKING,
    /** Remove bidirectional control marks (U+200E, U+200F, U+061C) from the output. */
    val stripBidiMarks: Boolean = true,
) {
    public enum class SpaceStyle(public val char: Char) {
        REGULAR('\u0020'),
        NON_BREAKING('\u00A0'),
        NARROW_NON_BREAKING('\u202F'),
    }

    public companion object {
        /** Default policy: ASCII minus, non-breaking spaces, bidi marks stripped. */
        public val Default: NormalizationPolicy = NormalizationPolicy()
    }
}
