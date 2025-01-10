plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.library)
    alias(libs.plugins.moduleKind)
}

moduleKind = "api"

java.toolchain.languageVersion = JavaLanguageVersion.of(libs.versions.java.get())

android {
    namespace = "org.demo.feature.login.api"
    compileSdk = libs.versions.android.compileSDK.get().toInt()
}

dependencies {
    api(projects.demo.domainUsers)
}
