import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension

plugins {
    alias(libs.plugins.moduleKind)
    `maven-publish`
}

moduleKindConstraints {
    "api" compatibleWith "domain"
    "implementation" compatibleWith "api"
    "app" compatibleWith "implementation"
}

allprojects {
    group = "org.demo"
    version = "0.1.0"

    apply(plugin = "maven-publish")

    publishing.publications.register<MavenPublication>("default") {
        plugins.withId("java") {
            the<JavaPluginExtension>().withSourcesJar()
            from(components["java"])
        }
        plugins.withId("com.android.application") {
            the<BaseAppModuleExtension>().publishing.singleVariant("release") {
                withSourcesJar()
            }
            afterEvaluate { from(components["release"]) }
        }
        plugins.withId("com.android.library") {
            the<LibraryExtension>().publishing.multipleVariants {
                allVariants()
                withSourcesJar()
            }
            afterEvaluate { from(components["default"]) }
        }
    }

}
