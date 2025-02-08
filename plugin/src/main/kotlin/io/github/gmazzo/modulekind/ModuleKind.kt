@file:JvmName("ModuleKindAttr")
package io.github.gmazzo.modulekind

import org.gradle.api.attributes.Attribute
import java.io.Serializable

open class ModuleKind internal constructor(val value: String, val projectPath: String) : Serializable {

    override fun hashCode() = value.hashCode()

    override fun equals(other: Any?) = when (other) {
        is ModuleKind -> value == other.value || projectPath == other.projectPath
        is String -> value == other
        else -> false
    }

    override fun toString() = value

    companion object {

        val MODULE_KIND_ATTRIBUTE: Attribute<ModuleKind> = Attribute.of("io.github.gmazzo.modulekind", ModuleKind::class.java)

        const val MODULE_KIND_MISSING = "<missing>"

    }

}
