package io.github.gmazzo.modulekind

import io.github.gmazzo.modulekind.ModuleKind.Companion.MODULE_KIND_MISSING
import io.github.gmazzo.modulekind.ModuleKindConstraintsExtension.OnMissingKind
import io.github.gmazzo.modulekind.ModuleKindPlugin.Companion.isGradleSync
import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.kotlin.dsl.mapProperty

internal abstract class ModuleKindConstraintsService @Inject constructor(
    objects: ObjectFactory,
) :
    BuildService<BuildServiceParameters.None>,
    ModuleKindConstraintsExtension {

    abstract val constraintsAsMap: MapProperty<String, Set<String>>

    init {
        constraints.all {
            check(name.matches("\\w+".toRegex())) { "Module kind names may only contain word characters" }

            compatibleWith.finalizeValueOnRead()
        }

        onMissingKind
            .convention(if (isGradleSync) OnMissingKind.WARN else OnMissingKind.FAIL)
            .finalizeValueOnRead()

        @Suppress("UNCHECKED_CAST")
        val constraintsAsMap =
            (objects.mapProperty(String::class, Set::class) as MapProperty<String, Set<String>>).apply {
                constraints.all { put(name, compatibleWith) }
                convention(
                    mapOf(
                        "api" to setOf(),
                        "implementation" to setOf("api"),
                        "monolith" to setOf("monolith", "implementation")
                    )
                )
                finalizeValueOnRead()
            }

        with(this.constraintsAsMap) {
            value(constraintsAsMap.map {
                (it.keys + it.values.flatten()).associateWith { kind -> it.resolveCompatibility(kind) }
            })
            finalizeValueOnRead()
            disallowChanges()
        }
    }

    private fun Map<String, Set<String>>.resolveCompatibility(
        forKind: String,
        into: MutableSet<String> = linkedSetOf(),
    ): Set<String> {
        if (forKind == MODULE_KIND_MISSING) return setOf(MODULE_KIND_MISSING)
        get(forKind)?.forEach { if (into.add(it)) resolveCompatibility(it, into) }
        return into
    }

}
