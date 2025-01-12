package io.github.gmazzo.modulekind

import com.android.build.gradle.BaseExtension
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.the
import org.gradle.testfixtures.ProjectBuilder
import kotlin.apply as kotlinApply

sealed class TestScenario(android: Boolean = false) {

    data object Default : TestScenario()
    data object Android : TestScenario(android = true)
    data object Invalid : TestScenario() {
        val violatingModule = createProject(name = "violatingModule", kind = "implementation", feature1Impl)
    }

    val rootProject = createProject(name = "root", kind = null)
    val feature1Api = createProject(name = "feature1-api", kind = "api")
    val feature1Impl = createProject(name = "feature1-impl", kind = "implementation", feature1Api)
    val feature2Api = createProject(name = "feature2-api", kind = "api", android = android)
    val feature2Impl = createProject(name = "feature2-impl", kind = "implementation", feature2Api, android = android)
    val feature3Api = createProject(name = "feature3-api", kind = "api", android = android)
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
    ): Project = ProjectBuilder.builder().withName(name).withParent(rootProject).build().kotlinApply {
        apply(plugin =
            if (android) if (name == "monolith") "com.android.application" else "com.android.library"
            else if (name == "monolith") "java" else "java-library"
        )
        apply(plugin = "io.github.gmazzo.modulekind")

        moduleKind.value(kind)

        dependsOn.forEach {
            dependencies {
                "implementation"(it)
            }
        }

        if (android) {
            configure<BaseExtension> {
                compileSdkVersion(30)
                namespace = project.name.replace('-', '.')
            }
        }

        getTasksByName("tasks", false) // forces afterEvaluate blocks to be run
    }

    val Project.moduleKind get() = the<Property<String>>()
    val Project.moduleKindConstrains get() = the<ModuleKindConstrainsExtension>()

    companion object {
        operator fun <Scenario : TestScenario, Return> Scenario.invoke(block: Scenario.() -> Return) = block()
    }

}
