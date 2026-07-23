import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.vanniktech.publish)
    alias(libs.plugins.dokka)
}

kotlin {
    explicitApi = ExplicitApiMode.Strict
    jvmToolchain(17)

    androidTarget {
        publishLibraryVariants("release")
    }
    jvm()

    iosX64()
    iosArm64()
    iosSimulatorArm64()
    macosX64()
    macosArm64()

    js(IR) {
        browser()
        nodejs()
    }
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting

        // Shared Intl-based implementation for js + wasmJs.
        val jsAndWasmMain by creating { dependsOn(commonMain) }
        val jsMain by getting { dependsOn(jsAndWasmMain) }
        val wasmJsMain by getting { dependsOn(jsAndWasmMain) }

        // JVM needs ICU4J for compact/ordinal/spellout/measure/range parity.
        val jvmMain by getting {
            dependencies {
                implementation(libs.icu4j)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(projects.klocaleTestkit)
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(libs.robolectric)
                implementation(libs.junit)
            }
        }
    }
}

android {
    namespace = "dev.klocale.core"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        // Common GoldenTest/FormatStringTest need real ICU; on Android only the
        // Robolectric-backed AndroidGoldenTest can run them, so exclude the rest.
        unitTests.all { test ->
            test.filter {
                excludeTestsMatching("dev.klocale.GoldenTest")
                excludeTestsMatching("dev.klocale.FormatStringTest")
            }
        }
    }
}
