package io.github.gmazzo.modulekind

import org.gradle.api.provider.MapProperty

internal interface ModuleKindConstrainsExtensionInternal : ModuleKindConstrainsExtension {

    val constrainsAsMap: MapProperty<String, Set<String>>

}
