package io.github.gmazzo.modulekind

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.HasAndroidTest
import com.android.build.api.variant.HasUnitTest
import io.github.gmazzo.modulekind.ModuleKind.Companion.MODULE_KIND_ATTRIBUTE
import io.github.gmazzo.modulekind.ModuleKind.Companion.MODULE_KIND_MISSING
import io.github.gmazzo.modulekind.ModuleKindConstraintsExtension.OnMissingKind
import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.configuration.BuildFeatures
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
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
import org.gradle.kotlin.dsl.registerIfAbsent
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.typeOf
import org.gradle.kotlin.dsl.withType
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

public class ModuleKindPlugin @Inject constructor(
    private val gradle: Gradle,
    buildFeatures: BuildFeatures,
) : Plugin<ExtensionAware> {

    private val isolatedProjects = buildFeatures.isolatedProjects.active.get()

    override fun apply(target: ExtensionAware) {
        val service = gradle.sharedServices
            .registerIfAbsent("moduleKindConstraints", ModuleKindConstraintsService::class)
            .get()

        target.extensions
            .add(typeOf<ModuleKindConstraintsExtension>(), "moduleKindConstraints", service)

        when (target) {
            is Project -> target.configure(service)
            is Settings -> gradle.beforeProject { apply<ModuleKindPlugin>() }
        }
    }

    private fun Project.configure(service: ModuleKindConstraintsService) {
        if (!isolatedProjects) {
            subprojects {
                apply<ModuleKindPlugin>()
            }
        }

        val kind = createKindExtension(service.onMissingKind).map { ModuleKind(value = it, projectPath = path) }

        dependencies.attributesSchema.attribute(MODULE_KIND_ATTRIBUTE) {
            compatibilityRules.add(ModuleKindCompatibilityRule::class)
        }

        tasks.register<ModuleKindReportConstraintsTask>("moduleKindConstraints") {
            this@register.constraintsAsMap.value(service.constraintsAsMap).disallowChanges()
        }

        plugins.withType<JvmEcosystemPlugin> {
            val sourceSets = the<SourceSetContainer>()

            sourceSets.configureEach {
                configureKind(
                    service,
                    kind,
                    configurations(apiElementsConfigurationName, runtimeElementsConfigurationName, optional = true),
                    configurations(compileClasspathConfigurationName, runtimeClasspathConfigurationName),
                )
            }
        }

        plugins.withId("com.android.base") {
            with(AndroidSupport) { configure(this@ModuleKindPlugin, service, kind) }
        }

        plugins.withId("org.jetbrains.kotlin.multiplatform") {
            with(KMPSupport) { configure(this@ModuleKindPlugin, service, kind) }
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


    internal fun Project.configureKind(
        extension: ModuleKindConstraintsService,
        kind: Provider<ModuleKind>,
        elementsConfigurations: Sequence<Configuration>,
        classpathConfigurations: Sequence<Configuration>,
    ) = afterEvaluate {
        val compatibilities = kind
            .zip(extension.constraintsAsMap) { kind, constraints ->
                when (val compats = constraints[kind.value]) {
                    null -> {
                        val errMessage = "moduleKind '$kind' must be one of ${constraints.keys.joinToString { "'$it'" }}"

                        if (isGradleSync) { logger.error(errMessage); emptySet() }
                        else error(errMessage)
                    }
                    else -> compats
                }
            }
            .map { ModuleKind(value = it.joinToString(separator = "|"), projectPath = path) }

        elementsConfigurations.forEach { it.attributes.attributeProvider(MODULE_KIND_ATTRIBUTE, kind) }
        classpathConfigurations.forEach { it.attributes.attributeProvider(MODULE_KIND_ATTRIBUTE, compatibilities) }
    }

    internal fun Project.configurations(vararg names: String?, optional: Boolean = false) = names
        .asSequence()
        .filterNotNull()
        .mapNotNull { if (optional) configurations.findByName(it) else configurations.getByName(it) }

    private object AndroidSupport {

        fun Project.configure(
            plugin: ModuleKindPlugin,
            extension: ModuleKindConstraintsService,
            kind: Provider<ModuleKind>,
        ) = with(plugin) {
            extensions.getByName<AndroidComponentsExtension<*, *, *>>("androidComponents").onVariants { variant ->
                listOfNotNull(
                    variant,
                    (variant as? HasUnitTest)?.unitTest,
                    (variant as? HasAndroidTest)?.androidTest,
                ).forEach {
                    configureKind(
                        extension,
                        kind,
                        configurations("${it.name}ApiElements", "${it.name}RuntimeElements", optional = it != variant),
                        sequenceOf(it.compileConfiguration, it.runtimeConfiguration),
                    )
                }
            }
        }

    }

    private object KMPSupport {

        fun Project.configure(
            plugin: ModuleKindPlugin,
            extension: ModuleKindConstraintsService,
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

    internal companion object {

        internal val isGradleSync
            get() = System.getProperty("idea.sync.active") == "true"

    }

}
