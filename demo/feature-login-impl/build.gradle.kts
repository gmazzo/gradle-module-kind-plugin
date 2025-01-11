plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.library)
    alias(libs.plugins.moduleKind)
}

moduleKind = "implementation"

java.toolchain.languageVersion = JavaLanguageVersion.of(libs.versions.java.get())

android {
    namespace = "org.demo.feature.login.impl"
    compileSdk = libs.versions.android.compileSDK.get().toInt()
}

dependencies {
    implementation(projects.demo.featureLoginApi)
    implementation(projects.demo.featureListingImpl)
}
