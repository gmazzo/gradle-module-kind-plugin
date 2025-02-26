package io.github.gmazzo.modulekind

import org.gradle.testfixtures.ProjectBuilder
import org.gradle.kotlin.dsl.register
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ModuleKindReportConstraintsTaskTest {

    private val project = ProjectBuilder.builder().build()

    private val task = project.tasks.register<ModuleKindReportConstraintsTask>("testTask") {
        constraintsAsMap.put("api", emptySet<String>())
        constraintsAsMap.put("implementation", setOf("api"))
        constraintsAsMap.put("monolith", setOf("implementation", "api"))
    }

    @Test
    fun `rendering should not fail`() {
        task.get().printCompatibilityTable()
    }

    @Test
    fun `should render compatibility table`() = with(task.get()) {
        val output = buildString { renderCompatibilityTable() }

        assertEquals("""
            | `moduleKind`   | api | implementation | monolith |
            | -------------- | --- | -------------- | -------- |
            | api            | ❌   | ❌              | ❌        |
            | implementation | ✅   | ❌              | ❌        |
            | monolith       | ✅   | ✅              | ❌        |
        """.trimIndent().trim(), output.trim())
    }

}