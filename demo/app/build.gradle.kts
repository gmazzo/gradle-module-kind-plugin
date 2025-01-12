plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.application)
    alias(libs.plugins.moduleKind)
}

moduleKind = "app"

java.toolchain.languageVersion = JavaLanguageVersion.of(libs.versions.java.get())

android {
    namespace = "org.demo.app"
    compileSdk = libs.versions.android.compileSDK.get().toInt()
    defaultConfig.versionCode = 1
}

dependencies {
    implementation(projects.demo.featureLoginImpl)
    implementation(projects.demo.featureListingImpl)
}
