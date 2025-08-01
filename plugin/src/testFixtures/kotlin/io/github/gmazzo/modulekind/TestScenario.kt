package io.github.gmazzo.modulekind

import com.android.build.gradle.BaseExtension
import io.github.gmazzo.modulekind.ModuleKindConstraintsExtension.OnMissingKind
import kotlin.apply as kotlinApply
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.the
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

sealed class TestScenario(android: Boolean = false, kmp: Boolean = false) {

    data object Default : TestScenario()
    data object Android : TestScenario(android = true)
    data object KMP : TestScenario(kmp = true)
    data object WithMissing : TestScenario() {
        val feature1Utils = createProject(name = "utils", kind = null, feature1Api)

        init {
            rootProject.the<ModuleKindConstraintsExtension>().onMissingKind.set(OnMissingKind.WARN)
            feature1Impl.dependencies { "implementation"(feature1Utils) }
        }
    }

    data object Invalid : TestScenario() {
        val violatingModule = createProject(name = "violatingModule", kind = "implementation", feature1Impl)
    }

    val rootProject = createProject(name = "root", kind = null)
    val feature1Api = createProject(name = "feature1-api", kind = "api")
    val feature1Impl = createProject(name = "feature1-impl", kind = "implementation", feature1Api)
    val feature2Api = createProject(name = "feature2-api", kind = "api", android = android)
    val feature2Impl = createProject(
        name = "feature2-impl",
        kind = "implementation",
        feature2Api,
        android = android,
        kmp = kmp,
        fixtures = !android
    )
    val feature3Api = createProject(name = "feature3-api", kind = "api", android = android, kmp = kmp)
    val feature3Impl = createProject(
        name = "feature3-impl",
        kind = "implementation",
        feature3Api, feature1Api, feature2Api,
        android = android,
    )
    val monolith = createProject(
        name = "monolith",
        kind = "monolith",
        feature1Impl, feature2Impl, feature3Impl,
        android = android,
    )

    fun createProject(
        name: String,
        kind: String?,
        vararg dependsOn: Project,
        android: Boolean = false,
        kmp: Boolean = false,
        fixtures: Boolean = false,
    ): Project = ProjectBuilder.builder().withName(name).withParent(rootProject).build().kotlinApply {
        apply(
            plugin =
                if (android) if (name == "monolith") "com.android.application" else "com.android.library"
                else if (kmp) "org.jetbrains.kotlin.multiplatform"
                else if (name == "monolith") "java" else "java-library"
        )
        if (fixtures) {
            apply(plugin = "java-test-fixtures")
        }
        apply(plugin = "io.github.gmazzo.modulekind")

        moduleKind.value(kind)

        repositories.mavenCentral()

        dependsOn.forEach {
            dependencies {
                (if (kmp) "commonMainImplementation" else "implementation")(it)
            }
        }

        if (android) {
            configure<BaseExtension> {
                compileSdkVersion(30)
                namespace = project.name.replace('-', '.')
            }
        }
        if (kmp) {
            configure<KotlinMultiplatformExtension> {
                jvm()
            }
        }

        getTasksByName("tasks", false) // forces afterEvaluate blocks to be run
    }

    val Project.moduleKind get() = the<Property<String>>()

    companion object {
        operator fun <Scenario : TestScenario, Return> Scenario.invoke(block: Scenario.() -> Return) = block()
    }

}
