plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.samReceiver)
    alias(libs.plugins.gradle.pluginPublish)
    alias(libs.plugins.publicationsReport)
    jacoco
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

dependencies {
    fun DependencyHandlerScope.plugin(provider: Provider<PluginDependency>) =
        provider.get().run { "$pluginId:$pluginId.gradle.plugin:$version" }

    compileOnly(gradleKotlinDsl())
    compileOnly(plugin(libs.plugins.android.application))
    
    testImplementation(gradleKotlinDsl())
    testImplementation(gradleTestKit())
    testImplementation(plugin(libs.plugins.android.application))
}

testing.suites.withType<JvmTestSuite> {
    useJUnitJupiter()
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    reports.xml.required = true
}

tasks.publish {
    dependsOn(tasks.publishPlugins)
}
