package io.github.gmazzo.modulekind

import io.github.gmazzo.modulekind.TestScenario.Companion.invoke
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE
import org.gradle.api.internal.catalog.problems.ResolutionFailureProblemId
import org.gradle.internal.component.resolution.failure.exception.VariantSelectionByAttributesException
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getPlugin
import org.gradle.kotlin.dsl.the
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class ModuleKindPluginBaseTest(vararg scenarios: TestScenario) {

    val testCases = testCasesFor(*scenarios)

    @ParameterizedTest
    @MethodSource("getTestCases")
    fun `when graph is valid, classpath can be resolved`(
        scenario: TestScenario,
        target: Project,
        configuration: String,
        expectedDependencies: Set<Project>,
    ) = scenario {
        val expected = expectedDependencies.mapTo(linkedSetOf()) { it.path }
        val resolved = target.configurations[configuration].incoming
            .artifactView { attributes.attribute(ARTIFACT_TYPE_ATTRIBUTE, "jar") }
            .artifacts.artifacts
            .mapNotNullTo(linkedSetOf()) { (it.id.componentIdentifier as? ProjectComponentIdentifier)?.projectPath }

        assertEquals(expected, resolved)
    }

    @Test
    fun `when an implementation depends on another implementation, it fails`() = TestScenario.Invalid {
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

    @Test
    fun `constraints instance is the same on all modules`() = TestScenario.Default {
        val expected = rootProject.the<ModuleKindConstraintsExtension>()

        rootProject.allprojects {
            val actual = with(rootProject.plugins.getPlugin(ModuleKindPlugin::class)) {
                rootProject.findOrCreateExtension()
            }

            assertSame(expected, actual)
        }
    }

    protected fun testCasesFor(vararg scenarios: TestScenario) = scenarios.flatMap { scenario ->
        with(scenario) {
            sequenceOf(
                testCaseFor(scenario, feature1Api, "compileClasspath"),
                testCaseFor(scenario, feature1Api, "runtimeClasspath"),
                testCaseFor(scenario, feature2Api, "compileClasspath"),
                testCaseFor(scenario, feature2Api, "runtimeClasspath"),
                testCaseFor(scenario, feature3Api, "compileClasspath"),
                testCaseFor(scenario, feature3Api, "runtimeClasspath"),
                testCaseFor(scenario, feature1Impl, "compileClasspath", feature1Api),
                testCaseFor(scenario, feature1Impl, "runtimeClasspath", feature1Api),
                testCaseFor(scenario, feature2Impl, "compileClasspath", feature2Api),
                testCaseFor(scenario, feature2Impl, "runtimeClasspath", feature2Api),
                testCaseFor(scenario, feature3Impl, "compileClasspath", feature1Api, feature2Api, feature3Api),
                testCaseFor(scenario, feature3Impl, "runtimeClasspath", feature1Api, feature2Api, feature3Api),
                testCaseFor(scenario, monolith, "compileClasspath", feature1Impl, feature2Impl, feature3Impl),
                testCaseFor(
                    scenario, monolith, "runtimeClasspath",
                    feature1Impl, feature2Impl, feature3Impl, feature1Api, feature2Api, feature3Api,
                ),
            ).flatten()
        }
    }

    private fun testCaseFor(
        scenario: TestScenario,
        project: Project,
        configuration: String,
        vararg dependencies: Project
    ) : Sequence<Array<Any>> =
        if (project.plugins.hasPlugin("com.android.base")) {
            val configSuffix = configuration.replaceFirstChar { it.uppercase() }
            val dependenciesSet = dependencies.toSet()

            sequenceOf("debug", "release")
                .map { arrayOf(scenario, project, "$it$configSuffix", dependenciesSet) }
        } else if (project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
            val configSuffix = configuration.replaceFirstChar { it.uppercase() }

            sequenceOf(arrayOf(scenario, project, "jvm$configSuffix", dependencies.toSet()))

        } else
            sequenceOf(arrayOf(scenario, project, configuration, dependencies.toSet()))

}
