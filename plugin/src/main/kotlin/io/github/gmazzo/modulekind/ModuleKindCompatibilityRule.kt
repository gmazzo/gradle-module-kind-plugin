package io.github.gmazzo.modulekind

import io.github.gmazzo.modulekind.ModuleKind.Companion.MODULE_KIND_MISSING
import org.gradle.api.attributes.AttributeCompatibilityRule
import org.gradle.api.attributes.CompatibilityCheckDetails

class ModuleKindCompatibilityRule : AttributeCompatibilityRule<ModuleKind> {

    override fun execute(details: CompatibilityCheckDetails<ModuleKind>): Unit = with(details) {
        if (producerValue?.value == MODULE_KIND_MISSING ||
            consumerValue?.value == MODULE_KIND_MISSING ||
            producerValue?.projectPath == consumerValue?.projectPath ||
            producerValue?.value in consumerValue?.value?.split('|').orEmpty()) {
            compatible()
        }
    }

}
