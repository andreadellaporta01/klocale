import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
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

    js(IR) { browser() }
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { browser() }

    sourceSets {
        commonMain.dependencies {
            api(projects.klocaleCore)
            // api (not implementation): LocalNumberLocale exposes a compose.runtime type publicly.
            api(compose.runtime)
        }
    }
}

mavenPublishing {
    signAllPublications()
}

android {
    namespace = "dev.klocale.compose"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}
