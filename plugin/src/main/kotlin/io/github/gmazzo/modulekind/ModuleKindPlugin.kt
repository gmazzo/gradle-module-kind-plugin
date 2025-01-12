package io.github.gmazzo.modulekind

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JvmEcosystemPlugin
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.add
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.typeOf
import org.gradle.kotlin.dsl.withType
import org.jetbrains.annotations.VisibleForTesting

class ModuleKindPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {

        val extension = findOrCreateExtension()
        val isRoot = project.extensions.findByType<ModuleKindConstrainsExtension>() != null
        val kind = createKindExtension(warnIfMissingKind = !isRoot)

        dependencies.attributesSchema.attribute(MODULE_KIND_ATTRIBUTE) {
            compatibilityRules.add(ModuleKindCompatibilityRule::class)
        }

        plugins.withType<JvmEcosystemPlugin> {
            the<SourceSetContainer>().configureEach {
                configureKind(
                    extension,
                    kind,
                    configurations(apiElementsConfigurationName, runtimeElementsConfigurationName, optional = true),
                    configurations(compileClasspathConfigurationName, runtimeClasspathConfigurationName),
                )
            }
        }
        plugins.withId("com.android.base") {
            with(ModuleKindAndroidSupport) {
                configure(this@ModuleKindPlugin, extension, kind)
            }
        }
    }

    private fun Project.createKindExtension(warnIfMissingKind: Boolean) = objects.property<String>().apply {
        convention(provider {
            if (warnIfMissingKind) {
                logger.warn(
                    "'moduleKind' not set for project '{}'. i.e. 'moduleKind = \"implementation\"'",
                    path,
                )
            }
            MODULE_KIND_MISSING
        })
        finalizeValueOnRead()
        extensions.add(typeOf<Property<String>>(), "moduleKind", this)
    }

    @VisibleForTesting
    internal fun Project.findOrCreateExtension() = generateSequence(project, Project::getParent)
        .mapNotNull { it.extensions.findByType<ModuleKindConstrainsExtension>() }
        .ifEmpty { sequenceOf(createExtension()) }
        .first() as ModuleKindConstrainsExtensionInternal

    private fun Project.createExtension() = extensions.create(
        ModuleKindConstrainsExtension::class,
        "moduleKindConstrains",
        ModuleKindConstrainsExtensionInternal::class
    ).apply {
        constrains.all {
            check('|' !in name) { "Character '|' is not allowed in module kind names" }

            compatibleWith.finalizeValueOnRead()
        }

        with((this as ModuleKindConstrainsExtensionInternal).constrainsAsMap) {
            constrains.all { put(name, compatibleWith) }
            convention(
                mapOf(
                    "api" to setOf("api"),
                    "implementation" to setOf("api"),
                    "monolith" to setOf("monolith", "implementation")
                )
            )
            finalizeValueOnRead()
        }
    }

    internal fun Project.configureKind(
        extension: ModuleKindConstrainsExtensionInternal,
        kind: Provider<String>,
        elementsConfigurations: Sequence<Configuration>,
        classpathConfigurations: Sequence<Configuration>,
    ) {

        val compatibilities = extension.constrainsAsMap
            .zip(kind) { constraints, kind -> constraints.resolveCompatibility(kind) }
            .map { it.joinToString(separator = "|") }

        elementsConfigurations.forEach { it.attributes.attributeProvider(MODULE_KIND_ATTRIBUTE, kind) }
        classpathConfigurations.forEach { it.attributes.attributeProvider(MODULE_KIND_ATTRIBUTE, compatibilities) }
    }

    private fun Map<String, Set<String>>.resolveCompatibility(
        forKind: String,
        into: MutableSet<String> = linkedSetOf(),
    ): Set<String> {
        if (forKind == MODULE_KIND_MISSING) return setOf(MODULE_KIND_MISSING)
        get(forKind)?.forEach { if (into.add(it)) resolveCompatibility(it, into) }
        return into
    }

    internal fun Project.configurations(vararg names: String, optional: Boolean = false) = names
        .asSequence()
        .mapNotNull { if (optional) configurations.findByName(it) else configurations.getByName(it) }

}
