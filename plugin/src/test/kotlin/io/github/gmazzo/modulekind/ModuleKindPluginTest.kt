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
        expectedKindFailure: String? = null,
    ) = fixtures {
        if (expectedKindFailure == null) {
            val expected = expectedDependencies.mapTo(linkedSetOf()) { it.path }
            val resolved = target.configurations[configuration].incoming.artifacts.artifacts
                .mapTo(linkedSetOf()) { (it.id.componentIdentifier as ProjectComponentIdentifier).projectPath }

            assertEquals(expected, resolved)

        } else {
            testFailToResolve(target, expectedKind = expectedKindFailure)
        }
    }

    fun testCases() = sequenceOf(
        Fixtures.Default,
        Fixtures.Transitive,
        Fixtures.NonTransitive,
    ).flatMap { fixture ->
        with(fixture) {
            sequenceOf(
                arrayOf(fixture, feature1Api, "compileClasspath", emptySet<Project>(), null),
                arrayOf(fixture, feature1Api, "runtimeClasspath", emptySet<Project>(), null),
                arrayOf(fixture, feature2Api, "compileClasspath", emptySet<Project>(), null),
                arrayOf(fixture, feature2Api, "runtimeClasspath", emptySet<Project>(), null),
                arrayOf(fixture, feature3Api, "compileClasspath", emptySet<Project>(), null),
                arrayOf(fixture, feature3Api, "runtimeClasspath", emptySet<Project>(), null),
                arrayOf(fixture, feature1Impl, "compileClasspath", setOf(feature1Api), null),
                arrayOf(fixture, feature1Impl, "runtimeClasspath", setOf(feature1Api), null),
                arrayOf(fixture, feature2Impl, "compileClasspath", setOf(feature2Api), null),
                arrayOf(fixture, feature2Impl, "runtimeClasspath", setOf(feature2Api), null),
                arrayOf(fixture, feature3Impl, "compileClasspath", setOf(feature1Api, feature2Api, feature3Api), null),
                arrayOf(fixture, feature3Impl, "runtimeClasspath", setOf(feature1Api, feature2Api, feature3Api), null),
                arrayOf(fixture, monolith, "compileClasspath", setOf(feature1Impl, feature2Impl, feature3Impl), null),
                arrayOf(
                    fixture, monolith, "runtimeClasspath",
                    setOf(feature1Impl, feature2Impl, feature3Impl, feature1Api, feature2Api, feature3Api),
                    "monolith|implementation".takeIf { fixture == Fixtures.NonTransitive }
                ),
            )
        }
    }.iterator()

    @Test
    fun `when an implementation depends on another implementation, it fails`() = Fixtures.Invalid {
        testFailToResolve(violatingModule, expectedKind = "api")
    }

    private fun testFailToResolve(target: Project, expectedKind: String) {
        val exception = assertThrows<ResolveException> {
            target.configurations["runtimeClasspath"].resolve()
        }

        assertEquals(
            "Could not resolve all files for configuration '${target.path}:runtimeClasspath'.",
            exception.message
        )

        val failure = (exception.cause?.cause as VariantSelectionByAttributesException).failure
        assertEquals(ResolutionFailureProblemId.NO_COMPATIBLE_VARIANTS, failure.problemId)
        assertEquals(expectedKind, failure.requestedAttributes.getAttribute(MODULE_KIND_ATTRIBUTE))
    }

    sealed class Fixtures(transitive: Boolean?) {

        data object Default : Fixtures(transitive = null)
        data object Transitive : Fixtures(transitive = true)
        data object NonTransitive : Fixtures(transitive = false)
        data object Invalid : Fixtures(transitive = true) {
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

        init {
            transitive?.let(rootProject.moduleKindConstrains.transitiveCompatibility::value)
        }

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
        val Project.moduleKindConstrains get() = the<ModuleKindConstrainsExtension>()

        companion object {
            operator fun <Fixture : Fixtures> Fixture.invoke(block: Fixture.() -> Unit) = block()
        }

    }


}
