package io.github.gmazzo.modulekind

import io.github.gmazzo.modulekind.ModuleKindPluginTest.Fixtures.Companion.invoke
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.internal.catalog.problems.ResolutionFailureProblemId
import org.gradle.api.provider.Property
import org.gradle.internal.component.resolution.failure.exception.VariantSelectionByAttributesException
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.the
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.apply as kotlinApply

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ModuleKindPluginTest {

    @ParameterizedTest
    @MethodSource("testCases")
    fun `when graph is valid, classpath can be resolved`(
        fixtures: Fixtures,
        target: Project,
        configuration: String,
        expectedDependencies: Set<Project>,
    ) = fixtures {
        val expected = expectedDependencies.mapTo(linkedSetOf()) { it.path }
        val resolved = target.configurations[configuration].incoming.artifacts.artifacts
            .mapTo(linkedSetOf()) { (it.id.componentIdentifier as ProjectComponentIdentifier).projectPath }

        assertEquals(expected, resolved)
    }

    fun testCases() = Fixtures.Default {
        listOf(
            arrayOf(this, feature1Api, "compileClasspath", emptySet<Project>()),
            arrayOf(this, feature1Api, "runtimeClasspath", emptySet<Project>()),
            arrayOf(this, feature2Api, "compileClasspath", emptySet<Project>()),
            arrayOf(this, feature2Api, "runtimeClasspath", emptySet<Project>()),
            arrayOf(this, feature3Api, "compileClasspath", emptySet<Project>()),
            arrayOf(this, feature3Api, "runtimeClasspath", emptySet<Project>()),
            arrayOf(this, feature1Impl, "compileClasspath", setOf(feature1Api)),
            arrayOf(this, feature1Impl, "runtimeClasspath", setOf(feature1Api)),
            arrayOf(this, feature2Impl, "compileClasspath", setOf(feature2Api)),
            arrayOf(this, feature2Impl, "runtimeClasspath", setOf(feature2Api)),
            arrayOf(this, feature3Impl, "compileClasspath", setOf(feature1Api, feature2Api, feature3Api)),
            arrayOf(this, feature3Impl, "runtimeClasspath", setOf(feature1Api, feature2Api, feature3Api)),
            arrayOf(this, monolith, "compileClasspath", setOf(feature1Impl, feature2Impl, feature3Impl)),
            arrayOf(
                this, monolith, "runtimeClasspath",
                setOf(feature1Impl, feature2Impl, feature3Impl, feature1Api, feature2Api, feature3Api),
            ),
        )
    }

    @Test
    fun `when an implementation depends on another implementation, it fails`() = Fixtures.Invalid {
        val exception = assertThrows<ResolveException> {
            violatingModule.configurations["runtimeClasspath"].resolve()
        }

        assertEquals(
            "Could not resolve all files for configuration '${violatingModule.path}:runtimeClasspath'.",
            exception.message
        )

        val failure = (exception.cause?.cause as VariantSelectionByAttributesException).failure
        assertEquals(ResolutionFailureProblemId.NO_COMPATIBLE_VARIANTS, failure.problemId)
        assertEquals("api", failure.requestedAttributes.getAttribute(MODULE_KIND_ATTRIBUTE))
    }

    sealed class Fixtures {

        data object Default : Fixtures()
        data object Invalid : Fixtures() {
            val violatingModule = createProject(name = "violatingModule", kind = "implementation", feature1Impl)
        }

        val rootProject = createProject(name = "root")
        val feature1Api = createProject(name = "feature1-api", kind = "api")
        val feature1Impl = createProject(name = "feature1-impl", kind = "implementation", feature1Api)
        val feature2Api = createProject(name = "feature2-api", kind = "api")
        val feature2Impl = createProject(name = "feature2-impl", kind = "implementation", feature2Api)
        val feature3Api = createProject(name = "feature3-api", kind = "api")
        val feature3Impl =
            createProject(name = "feature3-impl", kind = "implementation", feature3Api, feature1Api, feature2Api)
        val monolith = createProject(name = "monolith", kind = "monolith", feature1Impl, feature2Impl, feature3Impl)

        fun createProject(name: String, kind: String? = null, vararg dependsOn: Project): Project =
            ProjectBuilder.builder().withName(name).withParent(rootProject).build().kotlinApply {
                apply(plugin = if (name == "monolith") "java" else "java-library")
                apply(plugin = "io.github.gmazzo.modulekind")

                moduleKind.value(kind)

                dependsOn.forEach {
                    dependencies {
                        "implementation"(it)
                    }
                }
            }

        val Project.moduleKind get() = the<Property<String>>()

        companion object {
            operator fun <Fixture : Fixtures, Return> Fixture.invoke(block: Fixture.() -> Return) = block()
        }

    }


}
