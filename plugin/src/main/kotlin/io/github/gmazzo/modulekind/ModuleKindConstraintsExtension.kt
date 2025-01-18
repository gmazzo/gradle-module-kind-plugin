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
     * Defaults to [OnMissingKind.FAIL_EXCEPT_ROOT] (and [OnMissingKind.WARN] on Idea's Gradle Sync).
     */
    val onMissingKind: Property<OnMissingKind>

    /**
     * On `java` [org.gradle.api.component.SoftwareComponent], `apiElements` and `runtimeElements` configurations are used as both as outgoing variants and
     * publishing configuration.
     *
     * Since the `moduleKind` attribute is used to decorate the outgoing variants to restrict the hierarchy of modules,
     * the publications of those modules will be polluted with the attribute too.
     *
     * To prevent this, this flag will reconfigure the `java` component by creating a companion configuration of those,
     * which it should not have any side effect for the published components.
     * Other plugins also expecting to manipulate the component may not work as expected, so in that case, this behavior can be disabled.
     *
     * Defaults to `true`.
     */
    val removeKindAttributeFromPublications: Property<Boolean>

    enum class OnMissingKind {
        FAIL,
        WARN,
        IGNORE,
    }

}
