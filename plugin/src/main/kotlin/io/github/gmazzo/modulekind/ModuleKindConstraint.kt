package io.github.gmazzo.modulekind

import org.gradle.api.Named
import org.gradle.api.provider.SetProperty

public interface ModuleKindConstraint : Named {

    public val compatibleWith: SetProperty<String>

    public infix fun compatibleWith(other: String) {
        compatibleWith.add(other)
    }

    public infix fun compatibleWith(other: ModuleKindConstraint) {
        compatibleWith.add(other.name)
    }

    public fun compatibleWith(vararg others: String) {
        compatibleWith.addAll(others.toList())
    }

    public fun compatibleWith(vararg others: ModuleKindConstraint) {
        compatibleWith.addAll(others.map { it.name })
    }

}
