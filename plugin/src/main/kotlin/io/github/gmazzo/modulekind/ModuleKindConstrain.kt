package io.github.gmazzo.modulekind

import org.gradle.api.Named
import org.gradle.api.provider.SetProperty

@JvmDefaultWithoutCompatibility
interface ModuleKindConstrain : Named {

    val compatibleWith: SetProperty<String>

    fun compatibleWith(vararg compatibility: String) {
        compatibleWith.addAll(compatibility.toList())
    }

    fun compatibleWith(vararg compatibility: ModuleKindConstrain) {
        compatibleWith.addAll(compatibility.map { it.name })
    }

}
