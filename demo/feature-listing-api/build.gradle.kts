plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.moduleKind)
}

moduleKind = "api"

java.toolchain.languageVersion = JavaLanguageVersion.of(libs.versions.java.get())

dependencies {
    api(projects.demo.domainItems)
    api(projects.demo.domainUsers)
}
