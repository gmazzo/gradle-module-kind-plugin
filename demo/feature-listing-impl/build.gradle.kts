plugins {
    `java-library`
    `java-test-fixtures`
    alias(libs.plugins.kotlin.jvm)
}

moduleKind = "implementation"

java.toolchain.languageVersion = JavaLanguageVersion.of(libs.versions.java.get())

dependencies {
    implementation(projects.demo.featureListingApi)
}
