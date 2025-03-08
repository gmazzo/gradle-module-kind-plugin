plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.samReceiver)
    alias(libs.plugins.gradle.pluginPublish)
    alias(libs.plugins.publicationsReport)
    `java-test-fixtures`
    `jacoco-report-aggregation`
    signing
}

group = "io.github.gmazzo.modulekind"
description = "Gradle Module Kind Plugin"
version = providers
    .exec { commandLine("git", "describe", "--tags", "--always") }
    .standardOutput.asText.get().trim().removePrefix("v")

java.toolchain.languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
kotlin.compilerOptions.freeCompilerArgs = listOf("-Xjvm-default=all")
samWithReceiver.annotation(HasImplicitReceiver::class.qualifiedName!!)

gradlePlugin {
    website.set("https://github.com/gmazzo/gradle-module-kind-plugin")
    vcsUrl.set("https://github.com/gmazzo/gradle-module-kind-plugin")

    plugins {
        create("modulekind") {
            id = "io.github.gmazzo.modulekind"
            displayName = name
            implementationClass = "io.github.gmazzo.modulekind.ModuleKindPlugin"
            description = "Constraints a multi-module build dependency graph"
            tags.addAll("api", "implementaiton", "modules", "dependency", "dependencies", "dependency-graph", "constraints")
        }
    }
}

testing.suites {
    register<JvmTestSuite>("androidTest")
    register<JvmTestSuite>("kmpTest")
}

dependencies {
    fun plugin(provider: Provider<PluginDependency>) =
        provider.get().run { "$pluginId:$pluginId.gradle.plugin:$version" }

    compileOnly(gradleKotlinDsl())
    compileOnly(plugin(libs.plugins.android.application))
    compileOnly(plugin(libs.plugins.kotlin.multiplatform))

    testFixturesApi(gradleKotlinDsl())
    testFixturesApi(gradleTestKit())
    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.params)
    testFixturesCompileOnly(plugin(libs.plugins.android.library))
    testFixturesCompileOnly(plugin(libs.plugins.kotlin.multiplatform))

    testImplementation(testFixtures(project))

    "androidTestImplementation"(testFixtures(project))
    "androidTestImplementation"(plugin(libs.plugins.android.library))

    "kmpTestImplementation"(testFixtures(project))
    "kmpTestImplementation"(plugin(libs.plugins.kotlin.multiplatform))
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project

    useInMemoryPgpKeys(signingKey, signingPassword)
    publishing.publications.configureEach(::sign)
    tasks.withType<Sign>().configureEach { enabled = signingKey != null }
}

components.named<AdhocComponentWithVariants>("java") {
    val testFixtures by sourceSets

    withVariantsFromConfiguration(configurations.getByName(testFixtures.apiElementsConfigurationName)) { skip() }
    withVariantsFromConfiguration(configurations.getByName(testFixtures.runtimeElementsConfigurationName)) { skip() }
}

testing.suites.withType<JvmTestSuite> {
    useJUnitJupiter()
}

tasks.check {
    dependsOn(tasks.withType<JacocoReport>())
}

tasks.withType<JacocoReport> {
    reports.xml.required = true
}

tasks.publish {
    dependsOn(tasks.publishPlugins)
}
