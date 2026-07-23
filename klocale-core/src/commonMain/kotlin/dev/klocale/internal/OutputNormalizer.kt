package dev.klocale.internal

import dev.klocale.NormalizationPolicy

internal object OutputNormalizer {
    private const val UNICODE_MINUS = '\u2212'
    private const val NBSP = '\u00A0'
    private const val NARROW_NBSP = '\u202F'
    private const val LRM = '\u200E'
    private const val RLM = '\u200F'
    private const val ALM = '\u061C'

    fun apply(
        raw: String,
        policy: NormalizationPolicy,
    ): String {
        val space = policy.groupingSpace.char
        val sb = StringBuilder(raw.length)
        for (ch in raw) {
            when {
                policy.stripBidiMarks && (ch == LRM || ch == RLM || ch == ALM) -> Unit
                policy.useAsciiMinus && ch == UNICODE_MINUS -> sb.append('-')
                ch == NBSP || ch == NARROW_NBSP -> sb.append(space)
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }
}
