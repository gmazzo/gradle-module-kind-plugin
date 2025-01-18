package io.github.gmazzo.modulekind

import com.android.build.api.variant.AndroidComponentsExtension
import io.github.gmazzo.modulekind.ModuleKindConstraintsExtension.OnMissingKind
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.plugins.JvmEcosystemPlugin
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.add
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.typeOf
import org.gradle.kotlin.dsl.withType
import org.jetbrains.annotations.VisibleForTesting

class ModuleKindPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        val extension = findOrCreateExtension()

        val kind = createKindExtension(extension.onMissingKind)

        dependencies.attributesSchema.attribute(MODULE_KIND_ATTRIBUTE) {
            compatibilityRules.add(ModuleKindCompatibilityRule::class)
        }

        plugins.withType<JvmEcosystemPlugin> {
            val sourceSets = the<SourceSetContainer>()

            sourceSets.configureEach {
                configureKind(
                    extension,
                    kind,
                    configurations(apiElementsConfigurationName, runtimeElementsConfigurationName, optional = true),
                    configurations(compileClasspathConfigurationName, runtimeClasspathConfigurationName),
                )
            }

            plugins.withId("java") {
                components.named<AdhocComponentWithVariants>("java") {
                    if (extension.removeKindAttributeFromPublications.get()) {
                        val main by sourceSets

                        val apiElements = configurations.getByName(main.apiElementsConfigurationName)
                        val runtimeElements = configurations.getByName(main.runtimeElementsConfigurationName)

                        withVariantsFromConfiguration(apiElements) { skip() }
                        withVariantsFromConfiguration(runtimeElements) { skip() }

                        addVariantsFromConfiguration(cloneConfigForPublication(apiElements)) { mapToMavenScope("compile") }
                        addVariantsFromConfiguration(cloneConfigForPublication(runtimeElements)) { mapToMavenScope("runtime") }
                    }
                }
            }
        }

        plugins.withId("com.android.base") {
            extensions.getByName<AndroidComponentsExtension<*, *, *>>("androidComponents").onVariants {
                configureKind(
                    extension,
                    kind,
                    configurations("${it.name}ApiElements", "${it.name}RuntimeElements"),
                    sequenceOf(it.compileConfiguration, it.runtimeConfiguration),
                )
            }
        }
    }

    private fun Project.createKindExtension(onMissingKind: Property<OnMissingKind>) = objects.property<String>().apply {
        convention(onMissingKind.map { onMissingKind ->
            fun message() = "'moduleKind' not set for project '$path'. i.e. 'moduleKind = \"implementation\"'"

            when (onMissingKind) {
                OnMissingKind.FAIL -> error(message())
                OnMissingKind.WARN -> logger.warn(message())
                OnMissingKind.IGNORE -> {} // no-op
            }
            return@map MODULE_KIND_MISSING
        })
        finalizeValueOnRead()
        extensions.add(typeOf<Property<String>>(), "moduleKind", this)
    }

    @VisibleForTesting
    internal fun Project.findOrCreateExtension() = generateSequence(project, Project::getParent)
        .mapNotNull { it.extensions.findByType<ModuleKindConstraintsExtension>() }
        .ifEmpty { sequenceOf(createExtension()) }
        .first() as ModuleKindConstraintsExtensionInternal

    private fun Project.createExtension() = extensions.create(
        ModuleKindConstraintsExtension::class,
        "moduleKindConstraints",
        ModuleKindConstraintsExtensionInternal::class
    ).apply {
        constraints.all {
            check(name.matches("\\w+".toRegex())) { "Module kind names may only contain word characters" }

            compatibleWith.finalizeValueOnRead()
        }

        val isGradleSync = provider { gradle.taskGraph.allTasks.any { it.name == "prepareKotlinBuildScriptModel" } }

        onMissingKind
            .convention(isGradleSync.map { if (it) OnMissingKind.WARN else OnMissingKind.FAIL })
            .finalizeValueOnRead()

        removeKindAttributeFromPublications
            .convention(true)
            .finalizeValueOnRead()

        with((this as ModuleKindConstraintsExtensionInternal).constraintsAsMap) {
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
    }

    internal fun Project.configureKind(
        extension: ModuleKindConstraintsExtensionInternal,
        kind: Provider<String>,
        elementsConfigurations: Sequence<Configuration>,
        classpathConfigurations: Sequence<Configuration>,
    ) {
        val compatibilities = extension.constraintsAsMap
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

    private fun Project.cloneConfigForPublication(configuration: Configuration) =
        configurations.create("${configuration.name}Publication") {
            isCanBeConsumed = false
            isCanBeResolved = false

            extendsFrom(configuration)
            attributes {
                configuration.attributes.keySet().forEach { attr ->
                    if (attr != MODULE_KIND_ATTRIBUTE) {
                        @Suppress("UNCHECKED_CAST")
                        attributeProvider(
                            attr as Attribute<Any>,
                            provider { configuration.attributes.getAttribute<Any>(attr) })
                    }
                }
            }
            outgoing.artifacts(provider { configuration.artifacts })
        }

}
