plugins {
    alias(libs.plugins.android.library)
}

moduleKind = "implementation"

java.toolchain.languageVersion = JavaLanguageVersion.of(libs.versions.java.get())

android {
    namespace = "org.demo.feature.login.impl"
    compileSdk = libs.versions.android.compileSDK.get().toInt()
}

dependencies {
    implementation(projects.demo.featureLoginApi)
    testImplementation(testFixtures(projects.demo.featureListingApi))
    // implementation(projects.demo.featureListingImpl) // TODO uncomment to see the check fail
}
