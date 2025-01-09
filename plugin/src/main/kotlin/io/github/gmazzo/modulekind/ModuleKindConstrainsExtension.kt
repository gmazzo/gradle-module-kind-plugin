package io.github.gmazzo.modulekind

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.provider.Property

@JvmDefaultWithoutCompatibility
interface ModuleKindConstrainsExtension {

    val constrains: NamedDomainObjectContainer<ModuleKindConstrain>

    /**
     * Declares that a module kind of [kind] is compatible with another of kind [compatibleWith] (and possible [andOthers]).
     */
    fun compatibility(kind: String, compatibleWith: String, vararg andOthers: String) {
        constrains.maybeCreate(kind).compatibleWith(compatibleWith, *andOthers)
    }

    /**
     * Declares that a module kind of [kind] is compatible with another of kind [compatibleWith] (and possible [andOthers]).
     */
    fun compatibility(kind: String, compatibleWith: ModuleKindConstrain, vararg andOthers: ModuleKindConstrain) {
        constrains.maybeCreate(kind).compatibleWith(compatibleWith, *andOthers)
    }

    /**
     * Where the module kind compatibility is transitive declared at [ModuleKindConstrain.compatibleWith] or not.
     *
     * For instance, given:
     * ```kotlin
     * moduleKindConstrains {
     *    compatibility("implementation", "api")
     *    compatibility("monolith", "implementation")
     *    transitiveCompatibility = true
     * }
     * ```
     *
     * Will imply that `monolith` is compatible with `api` as well, as `implementation` is compatible with `api`.
     */
    val transitiveCompatibility: Property<Boolean>

    /**
     * Declares that a module kind of [this] is compatible with another of kind [compatibleWith].
     */
    infix fun String.compatibleWith(other: String) =
        compatibility(this, other)

    /**
     * Declares that a module kind of [this] is compatible with another of kind [compatibleWith].
     */
    infix fun String.compatibleWith(other: ModuleKindConstrain) =
        compatibility(this, other)


}
