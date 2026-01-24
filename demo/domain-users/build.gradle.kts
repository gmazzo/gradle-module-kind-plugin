plugins {
    alias(libs.plugins.android.multiplatform)
    alias(libs.plugins.kotlin.multiplatform)
}

moduleKind = "domain"

java.toolchain.languageVersion = JavaLanguageVersion.of(libs.versions.java.get())

kotlin {
    jvm()
    androidLibrary {
        namespace = "org.demo.domain.items"
        compileSdk = libs.versions.android.compileSDK.get().toInt()
    }
}
