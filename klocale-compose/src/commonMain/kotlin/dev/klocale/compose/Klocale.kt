package dev.klocale.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import dev.klocale.NormalizationPolicy
import dev.klocale.NumberFormatter
import dev.klocale.NumberLocale
import dev.klocale.NumberStyle

/**
 * The [NumberLocale] used by [rememberNumberFormatter] when none is passed explicitly.
 * Defaults to the system locale; override a subtree with [ProvideNumberLocale].
 */
public val LocalNumberLocale: ProvidableCompositionLocal<NumberLocale> =
    compositionLocalOf { NumberLocale.current() }

/** Overrides [LocalNumberLocale] for [content]. */
@Composable
public fun ProvideNumberLocale(locale: NumberLocale, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalNumberLocale provides locale, content = content)
}

/**
 * Remembers a [NumberFormatter] for [style] and [locale], rebuilding only when an input changes.
 *
 * @throws dev.klocale.NumberFormatError if the style is invalid or unsupported for the locale.
 */
@Composable
public fun rememberNumberFormatter(
    style: NumberStyle,
    locale: NumberLocale = LocalNumberLocale.current,
    policy: NormalizationPolicy = NormalizationPolicy.Default,
): NumberFormatter = remember(style, locale, policy) {
    NumberFormatter.orThrow(style, locale, policy)
}
