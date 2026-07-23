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
            val formatter = NumberFormatter.orThrow(case.style, locale)
            val actual = formatter.format(case.input)
            val expected = case.expectedFor(currentPlatform)
            if (actual != expected) {
                failures += "[${case.id}@$currentPlatform] expected <$expected> but was <$actual>"
            }
        }
        assertEquals(emptyList(), failures, "Golden mismatches:\n" + failures.joinToString("\n"))
    }
}
