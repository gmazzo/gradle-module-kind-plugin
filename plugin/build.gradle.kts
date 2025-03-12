plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.samReceiver)
    alias(libs.plugins.dokka)
    alias(libs.plugins.axion.release)
    alias(libs.plugins.mavenPublish)
    alias(libs.plugins.gradle.pluginPublish)
    alias(libs.plugins.publicationsReport)
    `java-test-fixtures`
    `jacoco-report-aggregation`
}

group = "io.github.gmazzo.modulekind"
description = "Constraints a multi-module build dependency graph"
version = scmVersion.version

java.toolchain.languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
kotlin.compilerOptions.freeCompilerArgs = listOf("-Xjvm-default=all")
samWithReceiver.annotation(HasImplicitReceiver::class.qualifiedName!!)

val originUrl = providers
    .exec { commandLine("git", "remote", "get-url", "origin") }
    .standardOutput.asText.map { it.trim() }

gradlePlugin {
    website = originUrl
    vcsUrl = originUrl

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

mavenPublishing {
    publishToMavenCentral("CENTRAL_PORTAL", automaticRelease = true)

    pom {
        name = "${rootProject.name}-${project.name}"
        description = provider { project.description }
        url = originUrl

        licenses {
            license {
                name = "MIT License"
                url = "https://opensource.org/license/mit/"
            }
        }

        developers {
            developer {
                id = "gmazzo"
                name = id
                email = "gmazzo65@gmail.com"
            }
        }

        scm {
            connection = originUrl
            developerConnection = originUrl
            url = originUrl
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

components.withType<AdhocComponentWithVariants>().configureEach {
    val testFixtures by sourceSets

    afterEvaluate {
        listOfNotNull(
            configurations.getByName(testFixtures.apiElementsConfigurationName),
            configurations.getByName(testFixtures.runtimeElementsConfigurationName),
            configurations.findByName(testFixtures.sourcesElementsConfigurationName),
            configurations.findByName(testFixtures.javadocElementsConfigurationName),
        ).forEach { withVariantsFromConfiguration(it) { skip() } }
    }
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

afterEvaluate {
    tasks.named<Jar>("javadocJar") {
        from(tasks.dokkaGeneratePublicationJavadoc)
    }
}

tasks.withType<PublishToMavenRepository>().configureEach {
    mustRunAfter(tasks.publishPlugins)
}

tasks.publishPlugins {
    enabled = "$version".matches("\\d+(\\.\\d+)+".toRegex())
}

tasks.publish {
    dependsOn(tasks.publishPlugins)
}
