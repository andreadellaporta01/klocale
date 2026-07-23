package dev.klocale

import dev.klocale.testkit.GOLDEN_CASES
import dev.klocale.testkit.Platform
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AndroidGoldenTest {

    @Test
    fun allGoldenCasesMatchOnAndroid() {
        val failures = mutableListOf<String>()
        for (case in GOLDEN_CASES) {
            val locale = NumberLocale.fromLanguageTag(case.locale).getOrThrow()
            val result = NumberFormatter(case.style, locale)
            if (Platform.ANDROID in case.unsupportedOn) {
                if (result.isSuccess) failures += "[${case.id}] expected UnsupportedStyle but succeeded"
                continue
            }
            val actual = result.getOrThrow().format(case.input)
            val expected = case.expectedFor(Platform.ANDROID)
            if (actual != expected) failures += "[${case.id}] expected <$expected> but was <$actual>"
        }
        assertEquals(emptyList(), failures, "Android golden mismatches:\n" + failures.joinToString("\n"))
    }
}
