package io.github.gmazzo.modulekind

import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.newInstance
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ModuleKindConstraintTest {

    private val objects = ProjectBuilder.builder().build().objects

    val constraint: ModuleKindConstraint = objects.newInstance<ModuleKindConstraint>("base").apply {
        compatibleWith.finalizeValueOnRead()
    }

    @Test
    fun `should add compatible constraint`() {
        constraint compatibleWith "other1"
        constraint compatibleWith objects.named("other2")
        constraint.compatibleWith("other3", "other4")
        constraint.compatibleWith(objects.named("other5"), objects.named("other6"))

        assertEquals(setOf("other1", "other2", "other3", "other4", "other5", "other6"), constraint.compatibleWith.get())
    }

}