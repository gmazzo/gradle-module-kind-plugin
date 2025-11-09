@file:JvmName("ModuleKindAttr")

package io.github.gmazzo.modulekind

import java.io.Serializable
import org.gradle.api.Named
import org.gradle.api.attributes.Attribute

public class ModuleKind(
    public val value: String,
    public val projectPath: String,
) : Named, Serializable {

    override fun getName(): String = value

    override fun hashCode(): Int = value.hashCode()

    override fun equals(other: Any?): Boolean = when (other) {
        is ModuleKind -> value == other.value || projectPath == other.projectPath
        is String -> value == other
        is Named -> value == other.name
        else -> false
    }

    override fun toString(): String = value

    public companion object {

        public val MODULE_KIND_ATTRIBUTE: Attribute<ModuleKind> =
            Attribute.of("io.github.gmazzo.modulekind", ModuleKind::class.java)

        public const val MODULE_KIND_MISSING: String = "<missing>"

    }

}
