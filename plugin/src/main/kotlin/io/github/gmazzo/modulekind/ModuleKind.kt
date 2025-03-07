@file:JvmName("ModuleKindAttr")

package io.github.gmazzo.modulekind

import org.gradle.api.Named
import org.gradle.api.attributes.Attribute
import java.io.Serializable

class ModuleKind(
    val value: String,
    val projectPath: String,
) : Named, Serializable {

    override fun getName() = value

    override fun hashCode() = value.hashCode()

    override fun equals(other: Any?) = when (other) {
        is ModuleKind -> value == other.value || projectPath == other.projectPath
        is String -> value == other
        is Named -> value == other.name
        else -> false
    }

    override fun toString() = value

    companion object {

        val MODULE_KIND_ATTRIBUTE: Attribute<ModuleKind> =
            Attribute.of("io.github.gmazzo.modulekind", ModuleKind::class.java)

        const val MODULE_KIND_MISSING = "<missing>"

    }

}
