package dev.klocale

import dev.klocale.testkit.GOLDEN_CASES
import dev.klocale.testkit.currentPlatform
import kotlin.test.Test
import kotlin.test.assertEquals

class GoldenTest {

    @Test
    fun allGoldenCasesMatchOnThisPlatform() {
        val failures = mutableListOf<String>()
        for (case in GOLDEN_CASES) {
            val locale = NumberLocale.fromLanguageTag(case.locale).getOrThrow()
            val result = NumberFormatter(case.style, locale)
            if (currentPlatform in case.unsupportedOn) {
                if (result.isSuccess) {
                    failures += "[${case.id}@$currentPlatform] expected UnsupportedStyle but construction succeeded"
                }
                continue
            }
            val actual = result.getOrThrow().format(case.input)
            val expected = case.expectedFor(currentPlatform)
            if (actual != expected) {
                failures += "[${case.id}@$currentPlatform] expected <$expected> but was <$actual>"
            }
        }
        assertEquals(emptyList(), failures, "Golden mismatches:\n" + failures.joinToString("\n"))
    }
}
