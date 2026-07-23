plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.vanniktech.publish) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.binary.compat)
    alias(libs.plugins.ktlint) apply false
}

apiValidation {
    ignoredProjects += "klocale-testkit"
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
}
