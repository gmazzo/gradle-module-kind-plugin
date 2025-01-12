package io.github.gmazzo.modulekind

import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByName

internal object ModuleKindAndroidSupport {

    fun Project.configure(
        plugin: ModuleKindPlugin,
        extension: ModuleKindConstrainsExtensionInternal,
        kind: Provider<String>,
    ) = extensions.getByName<AndroidComponentsExtension<*, *, *>>("androidComponents").onVariants {
        with(plugin) {
            configureKind(
                extension,
                kind,
                configurations(),
                sequenceOf(it.compileConfiguration, it.runtimeConfiguration),
            )
        }
    }

}