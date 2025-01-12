package io.github.gmazzo.modulekind

import org.gradle.api.provider.MapProperty

internal interface ModuleKindConstraintsExtensionInternal : ModuleKindConstraintsExtension {

    val constraintsAsMap: MapProperty<String, Set<String>>

}
