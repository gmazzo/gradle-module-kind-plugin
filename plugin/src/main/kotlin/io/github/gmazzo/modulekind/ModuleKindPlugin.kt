package io.github.gmazzo.modulekind

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.plugins.JvmEcosystemPlugin
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.add
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.domainObjectContainer
import org.gradle.kotlin.dsl.mapProperty
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.typeOf

class ModuleKindPlugin : Plugin<Project> {

    private val attribute = Attribute.of("io.github.gmazzo.modulekind", String::class.java)

    private val constrainsType = typeOf<NamedDomainObjectContainer<ModuleKindConstrain>>()

    override fun apply(target: Project): Unit = with(target) {
        apply<JvmEcosystemPlugin>()

        val kind = objects.property<String>().apply {
            finalizeValueOnRead()
            extensions.add(typeOf<Property<String>>(), "moduleKind", this)
        }

        @Suppress("UNCHECKED_CAST")
        val constrains = (objects.mapProperty(String::class, Set::class) as MapProperty<String, Set<String>>).apply {
            findOrCreateSpecs().all { put(name, compatibleWith) }
            convention(
                mapOf(
                    "api" to setOf("api"),
                    "implementation" to setOf("api"),
                    "monolith" to setOf("implementation")
                )
            )
            finalizeValueOnRead()
        }

        val constrain = constrains.zip(kind) { constrains, kind -> constrains[kind] }
        val compatibilities = constrain.map { it.joinToString(separator = "|") }

        the<SourceSetContainer>().configureEach {
            sequenceOf(apiElementsConfigurationName, runtimeElementsConfigurationName)
                .mapNotNull(configurations::findByName)
                .forEach { it.attributes.attributeProvider(attribute, kind) }

            sequenceOf(compileClasspathConfigurationName, runtimeClasspathConfigurationName)
                .mapNotNull(configurations::findByName)
                .forEach { it.attributes.attributeProvider(attribute, compatibilities) }
        }

        dependencies.attributesSchema.attribute(attribute) {
            compatibilityRules.add(ModuleKindCompatibilityRule::class) {
                params(constrains)
            }
        }
    }

    private fun Project.findOrCreateSpecs() = generateSequence(project, Project::getParent)
        .mapNotNull { it.extensions.findByType(constrainsType) }
        .ifEmpty {
            sequenceOf(objects.domainObjectContainer(ModuleKindConstrain::class).apply {
                configureEach { compatibleWith.finalizeValueOnRead() }
                extensions.add(constrainsType, "moduleKindConstrains", this)
            })
        }
        .first()

}
