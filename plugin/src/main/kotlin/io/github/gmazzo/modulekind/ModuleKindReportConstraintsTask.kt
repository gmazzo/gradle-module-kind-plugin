package io.github.gmazzo.modulekind

import org.gradle.api.DefaultTask
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.jetbrains.annotations.VisibleForTesting

abstract class ModuleKindReportConstraintsTask : DefaultTask() {

    @get:Input
    abstract val constraintsAsMap: MapProperty<String, Set<String>>

    @TaskAction
    fun printCompatibilityTable() = buildString {
        renderCompatibilityTable()
    }

    @VisibleForTesting
    internal fun Appendable.renderCompatibilityTable() {
        val constraints = constraintsAsMap.get()
        val moduleKindHeader = "`moduleKind`"
        val firstColumnWidth = (sequenceOf (moduleKindHeader) + constraints.keys).maxOf { it.length  }
        val kinds = constraints.entries.sortedBy { (_, value) -> value.size }.map { it.key }

        append("| ")
        append(moduleKindHeader)
        repeat(firstColumnWidth - moduleKindHeader.length) { append(' ') }
        append(" |")
        kinds.forEach {
            append(' ')
            append(it)
            append(" |")
        }
        appendLine()

        append("| ")
        repeat(firstColumnWidth) { append('-') }
        append(" |")
        kinds.forEach {
            append(' ')
            repeat(it.length) { append('-') }
            append(" |")
        }
        appendLine()

        kinds.forEach { kind ->
            append("| ")
            append(kind)
            repeat(firstColumnWidth - kind.length) { append(' ') }
            append(" |")
            kinds.forEach {
                append(if (it in constraints[kind]!!) " ✅" else " ❌")
                repeat(it.length - 1) { append(' ') }
                append(" |")
            }
            appendLine()
        }
    }

}
