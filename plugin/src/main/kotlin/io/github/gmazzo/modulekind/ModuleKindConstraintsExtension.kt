package io.github.gmazzo.modulekind

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.provider.Property

public interface ModuleKindConstraintsExtension {

    public val constraints: NamedDomainObjectContainer<ModuleKindConstraint>

    /**
     * Declares that a module kind of [kind] is compatible with another of kind [compatibleWith] (and possible [andOthers]).
     */
    public fun compatibility(kind: String, compatibleWith: String, vararg andOthers: String) {
        constraints.maybeCreate(kind).compatibleWith(compatibleWith, *andOthers)
    }

    /**
     * Declares that a module kind of [kind] is compatible with another of kind [compatibleWith] (and possible [andOthers]).
     */
    public fun compatibility(kind: String, compatibleWith: ModuleKindConstraint, vararg andOthers: ModuleKindConstraint) {
        constraints.maybeCreate(kind).compatibleWith(compatibleWith, *andOthers)
    }

    /**
     * Declares that a module kind of [this] is compatible with another of kind [compatibleWith].
     */
    public infix fun String.compatibleWith(other: String): Unit =
        compatibility(this, other)

    /**
     * Declares that a module kind of [this] is compatible with another of kind [compatibleWith].
     */
    public infix fun String.compatibleWith(other: ModuleKindConstraint): Unit =
        compatibility(this, other)

    /**
     * Strategy when a module does not define a `moduleKind` property.
     *
     * Defaults to [OnMissingKind.FAIL] (and [OnMissingKind.WARN] on Idea's Gradle Sync).
     */
    public val onMissingKind: Property<OnMissingKind>

    public enum class OnMissingKind {
        FAIL,
        WARN,
        IGNORE,
    }

}
