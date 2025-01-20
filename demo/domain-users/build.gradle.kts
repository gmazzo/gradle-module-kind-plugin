plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
}

moduleKind = "domain"

java.toolchain.languageVersion = JavaLanguageVersion.of(libs.versions.java.get())

android {
    namespace = "org.demo.domain.items"
    compileSdk = libs.versions.android.compileSDK.get().toInt()
}

kotlin {
    jvm()
    androidTarget { publishAllLibraryVariants() }
}
