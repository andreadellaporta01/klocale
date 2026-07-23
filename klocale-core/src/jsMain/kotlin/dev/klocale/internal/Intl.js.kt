package dev.klocale.internal

internal actual fun intlFormat(
    locale: String,
    optionsJson: String,
    value: Double,
): String =
    js("new Intl.NumberFormat(locale, JSON.parse(optionsJson)).format(value)")

internal actual fun intlFormatString(
    locale: String,
    optionsJson: String,
    value: String,
): String =
    js("new Intl.NumberFormat(locale, JSON.parse(optionsJson)).format(value)")

internal actual fun resolvedLocale(locale: String): String =
    js("(function(){try{return new Intl.NumberFormat(locale===''?undefined:locale).resolvedOptions().locale;}catch(e){return '';}})()")

internal actual fun localeSeparators(locale: String): String =
    js("(function(){var p=new Intl.NumberFormat(locale).formatToParts(11111.1);var g='',d='';for(var i=0;i<p.length;i++){if(p[i].type==='group')g=p[i].value;if(p[i].type==='decimal')d=p[i].value;}return g+'|'+d;})()")

internal actual fun ordinalCategory(
    locale: String,
    n: Double,
): String =
    js("new Intl.PluralRules(locale, { type: 'ordinal' }).select(n)")

internal actual fun intlRelative(
    locale: String,
    numeric: String,
    style: String,
    unit: String,
    value: Double,
): String =
    js("new Intl.RelativeTimeFormat(locale, { numeric: numeric, style: style }).format(value, unit)")
