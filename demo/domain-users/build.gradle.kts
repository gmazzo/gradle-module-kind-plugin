plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
}

moduleKind = "domain"

java.toolchain.languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
