package io.github.gmazzo.modulekind

import com.android.build.api.variant.AndroidComponentsExtension
import io.github.gmazzo.modulekind.ModuleKind.Companion.MODULE_KIND_ATTRIBUTE
import io.github.gmazzo.modulekind.ModuleKind.Companion.MODULE_KIND_MISSING
import io.github.gmazzo.modulekind.ModuleKindConstraintsExtension.OnMissingKind
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JvmEcosystemPlugin
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.add
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.mapProperty
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.typeOf
import org.gradle.kotlin.dsl.withType
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class ModuleKindPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        val extension = findOrCreateExtension()

        subprojects {
            apply<ModuleKindPlugin>()
        }

        val kind = createKindExtension(extension.onMissingKind).map { ModuleKind(value = it, projectPath = path) }

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
        }

        plugins.withId("com.android.base") {
            with(AndroidSupport) { configure(this@ModuleKindPlugin, extension, kind) }
        }

        plugins.withId("org.jetbrains.kotlin.multiplatform") {
            with(KMPSupport) { configure(this@ModuleKindPlugin, extension, kind) }
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
    ).apply extension@{

        constraints.all {
            check(name.matches("\\w+".toRegex())) { "Module kind names may only contain word characters" }

            compatibleWith.finalizeValueOnRead()
        }

        val isGradleSync = provider { gradle.taskGraph.allTasks.any { it.name == "prepareKotlinBuildScriptModel" } }

        onMissingKind
            .convention(isGradleSync.map { if (it) OnMissingKind.WARN else OnMissingKind.FAIL })
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

        with((this as ModuleKindConstraintsExtensionInternal).constraintsAsMap) {
            value(constraintsAsMap.map {
                (it.keys + it.values.flatten()).associateWith { kind -> it.resolveCompatibility(kind) }
            })
            finalizeValueOnRead()
            disallowChanges()
        }

        tasks.register<ModuleKindReportConstraintsTask>("moduleKindConstraints") {
            this@register.constraintsAsMap.value(this@extension.constraintsAsMap).disallowChanges()
        }

    }

    internal fun Project.configureKind(
        extension: ModuleKindConstraintsExtensionInternal,
        kind: Provider<ModuleKind>,
        elementsConfigurations: Sequence<Configuration>,
        classpathConfigurations: Sequence<Configuration>,
    ) = afterEvaluate {
        val compatibilities = kind
            .zip(extension.constraintsAsMap) { kind, constraints ->
                checkNotNull(constraints[kind.value]) {
                    "moduleKind '$kind' must be one of ${constraints.keys.joinToString { "'$it'" }}"
                }
            }
            .map { ModuleKind(value = it.joinToString(separator = "|"), projectPath = path) }

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

    internal fun Project.configurations(vararg names: String?, optional: Boolean = false) = names
        .asSequence()
        .filterNotNull()
        .mapNotNull { if (optional) configurations.findByName(it) else configurations.getByName(it) }

    private object AndroidSupport {

        fun Project.configure(
            plugin: ModuleKindPlugin,
            extension: ModuleKindConstraintsExtensionInternal,
            kind: Provider<ModuleKind>,
        ) = with(plugin) {
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

    private object KMPSupport {

        fun Project.configure(
            plugin: ModuleKindPlugin,
            extension: ModuleKindConstraintsExtensionInternal,
            kind: Provider<ModuleKind>,
        ) = with(plugin) {
            extensions.getByName<KotlinMultiplatformExtension>("kotlin").targets.all target@{
                compilations.all comp@{
                    configureKind(
                        extension,
                        kind,
                        configurations(
                            this@target.apiElementsConfigurationName,
                            this@target.runtimeElementsConfigurationName,
                            optional = true
                        ),
                        configurations(compileDependencyConfigurationName, runtimeDependencyConfigurationName),
                    )
                }
            }
        }

    }

}
