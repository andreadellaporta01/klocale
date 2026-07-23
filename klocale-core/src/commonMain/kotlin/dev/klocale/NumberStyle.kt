package dev.klocale

/**
 * The formatting style. Every concrete style is a value type describing *what* to produce;
 * the locale describes *how* to render it.
 */
public sealed interface NumberStyle {

    /** Plain decimal number, e.g. `1,234.56`. */
    public data class Decimal(
        val minFractionDigits: Int = 0,
        val maxFractionDigits: Int = 3,
        val grouping: Boolean = true,
        val rounding: RoundingMode = RoundingMode.HALF_EVEN,
        val signDisplay: SignDisplay = SignDisplay.AUTO,
    ) : NumberStyle

    /** Currency amount, e.g. `$1,234.56` / `1.234,56 €`. */
    public data class Currency(
        val currencyCode: String,
        val presentation: Presentation = Presentation.SYMBOL,
        val minFractionDigits: Int? = null,
        val maxFractionDigits: Int? = null,
        val grouping: Boolean = true,
        val rounding: RoundingMode = RoundingMode.HALF_EVEN,
        val signDisplay: SignDisplay = SignDisplay.AUTO,
    ) : NumberStyle {
        public enum class Presentation { SYMBOL, ISO_CODE, ACCOUNTING }
    }

    /** Percentage, e.g. `42%`. [scale] selects whether the input is a ratio (0.42) or a value (42). */
    public data class Percent(
        val minFractionDigits: Int = 0,
        val maxFractionDigits: Int = 0,
        val scale: Scale = Scale.RATIO,
        val rounding: RoundingMode = RoundingMode.HALF_EVEN,
    ) : NumberStyle {
        public enum class Scale { RATIO, VALUE }
    }

    /** Abbreviated notation, e.g. `1.2K` (short) / `1.2 thousand` (long). */
    public data class Compact(
        val length: Length = Length.SHORT,
        val maxFractionDigits: Int = 1,
    ) : NumberStyle {
        public enum class Length { SHORT, LONG }
    }

    /** Scientific or engineering notation, e.g. `1.23E4`. */
    public data class Scientific(
        val engineering: Boolean = false,
        val maxFractionDigits: Int = 6,
    ) : NumberStyle

    /** Ordinal, e.g. `1st` (suffix) / `first` (word). */
    public data class Ordinal(
        val kind: Kind = Kind.SUFFIX,
    ) : NumberStyle {
        public enum class Kind { SUFFIX, WORD }
    }

    /** Spelled-out number, e.g. `one hundred twenty-three`. */
    public data class Spellout(
        val year: Boolean = false,
    ) : NumberStyle

    /** Measurement with a unit, e.g. `12.5 km`. */
    public data class Measure(
        val unit: MeasureUnit,
        val width: Width = Width.SHORT,
        val maxFractionDigits: Int = 3,
    ) : NumberStyle {
        public enum class Width { NARROW, SHORT, LONG }
    }

    /** Relative time, e.g. `in 3 days` / `2 hours ago`. */
    public data class RelativeTime(
        val unit: TimeUnit,
        val width: Width = Width.LONG,
        val numeric: Numeric = Numeric.AUTO,
    ) : NumberStyle {
        public enum class Width { NARROW, SHORT, LONG }
        public enum class Numeric { ALWAYS, AUTO }
    }
}

/** Units supported by [NumberStyle.Measure]. */
public enum class MeasureUnit {
    METER, KILOMETER, CENTIMETER, MILE, FOOT,
    GRAM, KILOGRAM, POUND, OUNCE,
    LITER, MILLILITER,
    CELSIUS, FAHRENHEIT,
    BYTE, KILOBYTE, MEGABYTE, GIGABYTE,
    SECOND, MINUTE, HOUR, DAY,
}

/** Time units supported by [NumberStyle.RelativeTime]. */
public enum class TimeUnit {
    SECOND, MINUTE, HOUR, DAY, WEEK, MONTH, QUARTER, YEAR,
}
