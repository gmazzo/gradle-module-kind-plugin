package io.github.gmazzo.modulekind

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
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
import javax.inject.Inject

class ModuleKindPlugin @Inject constructor(
    private val objects: ObjectFactory,
) : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {

        val extension = findOrCreateExtension()

        plugins.withType<JvmEcosystemPlugin> {
            val projectKind = createKindExtension(project)

            val isRoot = project.extensions.findByType<ModuleKindConstrainsExtension>() != null

            the<SourceSetContainer>().configureEach ss@{
                configure(
                    this,
                    this@ss.name,
                    "source set", projectKind, extension,
                    sequenceOf(apiElementsConfigurationName, runtimeElementsConfigurationName),
                    sequenceOf(compileClasspathConfigurationName, runtimeClasspathConfigurationName),
                    warnIfMissingKind = !isRoot
                )
            }

            dependencies.attributesSchema.attribute(MODULE_KIND_ATTRIBUTE) {
                compatibilityRules.add(ModuleKindCompatibilityRule::class)
            }
        }
    }

    private fun createKindExtension(
        on: ExtensionAware,
        convention: Provider<String>? = null,
    ) = objects.property<String>().apply {
        convention?.let(::convention)
        finalizeValueOnRead()
        on.extensions.add(typeOf<Property<String>>(), "moduleKind", this)
    }

    private fun Project.findOrCreateExtension() = generateSequence(project, Project::getParent)
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

    private fun Project.configure(
        target: ExtensionAware,
        targetName: String,
        targetDescription: String,
        defaultKind: Provider<String>,
        extension: ModuleKindConstrainsExtensionInternal,
        elementsConfigurations: Sequence<String>,
        classpathConfigurations: Sequence<String>,
        warnIfMissingKind: Boolean,
    ) {

        val missingValue = lazy {
            if (warnIfMissingKind) {
                logger.warn(
                    "'moduleKind' not set for project '{}', {} '{}'. i.e. 'moduleKind = \"implementation\"'",
                    path,
                    targetDescription,
                    targetName
                )
            }
            MODULE_KIND_MISSING
        }

        val kind = createKindExtension(target, convention = defaultKind)
            .orElse(provider(missingValue::value))

        val compatibilities = extension.constrainsAsMap
            .zip(kind) { constraints, kind -> constraints.resolveCompatibility(kind) }
            .map { it.joinToString(separator = "|") }

        elementsConfigurations
            .mapNotNull(configurations::findByName)
            .forEach { it.attributes.attributeProvider(MODULE_KIND_ATTRIBUTE, kind) }

        classpathConfigurations
            .mapNotNull(configurations::findByName)
            .forEach { it.attributes.attributeProvider(MODULE_KIND_ATTRIBUTE, compatibilities) }
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
