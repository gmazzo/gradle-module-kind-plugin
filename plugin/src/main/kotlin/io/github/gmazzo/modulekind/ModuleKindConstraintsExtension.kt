package io.github.gmazzo.modulekind

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.provider.Property

@JvmDefaultWithoutCompatibility
interface ModuleKindConstraintsExtension {

    val constraints: NamedDomainObjectContainer<ModuleKindConstraint>

    /**
     * Declares that a module kind of [kind] is compatible with another of kind [compatibleWith] (and possible [andOthers]).
     */
    fun compatibility(kind: String, compatibleWith: String, vararg andOthers: String) {
        constraints.maybeCreate(kind).compatibleWith(compatibleWith, *andOthers)
    }

    /**
     * Declares that a module kind of [kind] is compatible with another of kind [compatibleWith] (and possible [andOthers]).
     */
    fun compatibility(kind: String, compatibleWith: ModuleKindConstraint, vararg andOthers: ModuleKindConstraint) {
        constraints.maybeCreate(kind).compatibleWith(compatibleWith, *andOthers)
    }

    /**
     * Declares that a module kind of [this] is compatible with another of kind [compatibleWith].
     */
    infix fun String.compatibleWith(other: String) =
        compatibility(this, other)

    /**
     * Declares that a module kind of [this] is compatible with another of kind [compatibleWith].
     */
    infix fun String.compatibleWith(other: ModuleKindConstraint) =
        compatibility(this, other)

    /**
     * Strategy when a module does not define a `moduleKind` property.
     *
     * Defaults to [OnMissingKind.FAIL] (and [OnMissingKind.WARN] on Idea's Gradle Sync).
     */
    val onMissingKind: Property<OnMissingKind>

    enum class OnMissingKind {
        FAIL,
        WARN,
        IGNORE,
    }

}
