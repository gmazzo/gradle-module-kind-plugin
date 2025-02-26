package io.github.gmazzo.modulekind

import org.gradle.api.Project

class ModuleKindWithMissingPluginTest : ModuleKindPluginBaseTest(TestScenario.WithMissing) {

    override fun testCaseFor(
        scenario: TestScenario,
        project: Project,
        configuration: String,
        vararg dependencies: Project
    ) = super.testCaseFor(
        scenario, project, configuration,
        *when (project) {
            TestScenario.WithMissing.feature1Impl -> arrayOf(*dependencies, TestScenario.WithMissing.feature1Utils)
            TestScenario.WithMissing.monolith -> when (configuration) {
                "runtimeClasspath" -> arrayOf(*dependencies, TestScenario.WithMissing.feature1Utils)
                else -> dependencies
            }

            else -> dependencies
        }
    )

}
