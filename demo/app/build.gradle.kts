plugins {
    alias(libs.plugins.android.application)
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
