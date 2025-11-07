package io.github.gmazzo.modulekind

import org.gradle.api.Named
import org.gradle.api.provider.SetProperty

interface ModuleKindConstraint : Named {

    val compatibleWith: SetProperty<String>

    infix fun compatibleWith(other: String) {
        compatibleWith.add(other)
    }

    infix fun compatibleWith(other: ModuleKindConstraint) {
        compatibleWith.add(other.name)
    }

    fun compatibleWith(vararg others: String) {
        compatibleWith.addAll(others.toList())
    }

    fun compatibleWith(vararg others: ModuleKindConstraint) {
        compatibleWith.addAll(others.map { it.name })
    }

}
