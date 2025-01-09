package io.github.gmazzo.modulekind

import org.gradle.api.Project
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.the
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.apply as kotlinApply

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ModuleKindPluginTest {

    private val rootProject by lazy { ProjectBuilder.builder().build() }
    private val feature1Api by lazy { createProject(name = "feature1-api", kind = "api") }
    private val feature1Impl by lazy { createProject(name = "feature1-impl", kind = "implementation") }
    private val feature2Api by lazy { createProject(name = "feature2-api", kind = "api") }
    private val feature2Impl by lazy { createProject(name = "feature2-impl", kind = "implementation") }
    private val feature3Api by lazy { createProject(name = "feature3-api", kind = "api") }
    private val feature3Impl by lazy { createProject(name = "feature3-impl", kind = "implementation") }
    private val monolith by lazy { createProject(name = "monolith", kind = "monolith") }

    private fun createProject(name: String, kind: String) =
        ProjectBuilder.builder().withName(name).withParent(rootProject).build().kotlinApply {
            apply(plugin = if (name == "monolith") "java" else "java-library")
            apply(plugin = "io.github.gmazzo.modulekind")

            the<Property<String>>().value(kind)
        }

    @BeforeAll
    fun setup() {
        feature1Impl.dependencies {
            "implementation"(feature1Api)
        }
        feature2Impl.dependencies {
            "implementation"(feature2Api)
        }
        feature3Impl.dependencies {
            "implementation"(feature1Api)
            "implementation"(feature2Api)
            "implementation"(feature3Api)
        }
        monolith.dependencies {
            "implementation"(feature1Impl)
            "implementation"(feature2Impl)
            "implementation"(feature3Impl)
        }
    }

    @ParameterizedTest
    @MethodSource("testCases")
    fun `classpath can be resolved`(
        project: Project,
        configuration: String,
        expectedDependencies: Set<Project>,
    ) {
        val expected = expectedDependencies.mapTo(linkedSetOf()) { it.path }
        val resolved = project.configurations[configuration].incoming.artifacts.artifacts
            .mapTo(linkedSetOf()) { (it.id.componentIdentifier as ProjectComponentIdentifier).projectPath }

        assertEquals(expected, resolved)
    }

    fun testCases() = listOf(
        arrayOf(feature1Api, "compileClasspath", emptySet<Project>()),
        arrayOf(feature1Api, "runtimeClasspath", emptySet<Project>()),
        arrayOf(feature2Api, "compileClasspath", emptySet<Project>()),
        arrayOf(feature2Api, "runtimeClasspath", emptySet<Project>()),
        arrayOf(feature3Api, "compileClasspath", emptySet<Project>()),
        arrayOf(feature3Api, "runtimeClasspath", emptySet<Project>()),
        arrayOf(feature1Impl, "compileClasspath", setOf(feature1Api)),
        arrayOf(feature1Impl, "runtimeClasspath", setOf(feature1Api)),
        arrayOf(feature2Impl, "compileClasspath", setOf(feature2Api)),
        arrayOf(feature2Impl, "runtimeClasspath", setOf(feature2Api)),
        arrayOf(feature3Impl, "compileClasspath", setOf(feature1Api, feature2Api, feature3Api)),
        arrayOf(feature3Impl, "runtimeClasspath", setOf(feature1Api, feature2Api, feature3Api)),
        arrayOf(monolith, "compileClasspath", setOf(feature1Impl, feature2Impl, feature3Impl)),
        arrayOf(
            monolith,
            "runtimeClasspath",
            setOf(feature1Impl, feature2Impl, feature3Impl, feature1Api, feature2Api, feature3Api)
        ),
    )

}
