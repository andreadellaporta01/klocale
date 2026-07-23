# Klocale

Comprehensive, locale-aware number and value formatting for **Kotlin Multiplatform** — the
missing `NSNumberFormatter` / `Intl.NumberFormat` / ICU `NumberFormat` equivalent usable
straight from `commonMain`, with **consistent output across every target**.

Klocale delegates to each platform's native formatter (ICU on JVM/Android, `NSNumberFormatter`
on Apple, `Intl` on JS/WasmJs) and normalizes the cosmetic differences (minus glyphs,
non‑breaking spaces, bidi marks) so the same input produces the same string everywhere.

## Platforms

Android · iOS · macOS · JVM/Desktop · JS · WasmJs

## Install

```kotlin
// settings.gradle.kts -> mavenCentral()
dependencies {
    implementation("io.github.andreadellaporta01:klocale-core:0.1.0")
    // optional, for Compose Multiplatform:
    implementation("io.github.andreadellaporta01:klocale-compose:0.1.0")
}
```

## Usage

```kotlin
import dev.klocale.*

formatDecimal(1234.56, NumberLocale.ITALY)              // "1.234,56"
formatCurrency(1234.5, "EUR", NumberLocale.GERMANY)     // "1.234,50 €"
formatPercent(0.42)                                     // "42%"
formatCompact(1_200_000.0)                              // "1.2M"

val f = NumberFormatter.orThrow(
    NumberStyle.Currency("USD", presentation = NumberStyle.Currency.Presentation.ACCOUNTING),
    NumberLocale.US,
)
f.format(-1234.5)                                       // "($1,234.50)"
```

Construction returns a `Result` (invalid locale / malformed currency code / unsupported style),
while formatting a finite number never throws:

```kotlin
NumberFormatter.of(NumberStyle.Currency("US1"))         // Result.failure(InvalidCurrencyCode)
```

The currency code is validated for shape (three ASCII letters), not against the ISO 4217 registry.

### Compose Multiplatform

```kotlin
ProvideNumberLocale(NumberLocale.FRANCE) {
    val price = rememberNumberFormatter(NumberStyle.Currency("EUR"))
    Text(price.format(1234.5))                          // "1 234,50 €"
}
```

## Styles

| Style | Android | iOS/macOS | JVM | JS/WasmJs |
|---|:--:|:--:|:--:|:--:|
| Decimal | ✅ | ✅ | ✅ | ✅ |
| Currency (symbol / ISO / accounting) | ✅ | ✅ | ✅ | ✅ |
| Percent (ratio / value) | ✅ | ✅ | ✅ | ✅ |
| Scientific | ✅ | ✅ | ✅ | ✅ |
| Compact | ✅ | en only | ✅ | ✅ |
| Ordinal (suffix) | en only | ✅ | ✅ | en only |
| Spellout | — | ✅ | ✅ | — |
| Relative time | ✅ | ✅ (no quarter) | ✅ | ✅ |
| Measure | ✅ | — | ✅ | ✅ |

Unsupported combinations fail at construction with `NumberFormatError.UnsupportedStyle` — never
with wrong output. See the roadmap below for gaps being closed.

## Consistency

Output is verified by a shared golden-test table run on every backend — `jvmTest`,
`macosArm64Test`, `iosSimulatorArm64Test`, `jsNodeTest`, `wasmJsNodeTest` and the Android
Robolectric test — which also asserts that each backend correctly rejects the styles it cannot
support.

## Roadmap

- Apple `Measure` (via `NSMeasurementFormatter`)
- `Range` formatting (needs a two-value `formatRange(from, to)` API)
- `Ordinal` / `Spellout` word form and wider locale coverage

## License

Apache-2.0
