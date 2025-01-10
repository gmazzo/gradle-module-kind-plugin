package io.github.gmazzo.modulekind

import org.gradle.api.Plugin
import org.gradle.api.Project
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

class ModuleKindPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {

        val extension = findOrCreateExtension()

        plugins.withType<JvmEcosystemPlugin> {
            val projectKind = createKindExtension()

            val transitive by lazy(extension.transitiveCompatibility::get)

            fun Map<String, Set<String>>.resolveTo(
                kind: String,
                set: MutableSet<String> = linkedSetOf(),
            ): Set<String> {
                get(kind)?.forEach { if (set.add(it) && transitive) resolveTo(it, set) }
                return set
            }

            the<SourceSetContainer>().configureEach ss@{
                val ssKind = createKindExtension(this@ss, convention = projectKind)

                val compatibilities = extension.constrainsAsMap
                    .zip(ssKind, Map<String, Set<String>>::resolveTo)
                    .map { it.joinToString(separator = "|") }

                sequenceOf(apiElementsConfigurationName, runtimeElementsConfigurationName)
                    .mapNotNull(configurations::findByName)
                    .forEach { it.attributes.attributeProvider(MODULE_KIND_ATTRIBUTE, ssKind) }

                sequenceOf(compileClasspathConfigurationName, runtimeClasspathConfigurationName)
                    .mapNotNull(configurations::findByName)
                    .forEach { it.attributes.attributeProvider(MODULE_KIND_ATTRIBUTE, compatibilities) }
            }

            dependencies.attributesSchema.attribute(MODULE_KIND_ATTRIBUTE) {
                compatibilityRules.add(ModuleKindCompatibilityRule::class)
            }
        }
    }

    private fun Project.createKindExtension(
        on: ExtensionAware = this,
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

        transitiveCompatibility
            .convention(true)
            .finalizeValueOnRead()
    }

}
