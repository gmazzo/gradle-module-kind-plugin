plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.moduleKind)
}

moduleKind = "domain"

java.toolchain.languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
