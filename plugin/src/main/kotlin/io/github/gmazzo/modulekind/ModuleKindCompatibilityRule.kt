package io.github.gmazzo.modulekind

import org.gradle.api.attributes.AttributeCompatibilityRule
import org.gradle.api.attributes.CompatibilityCheckDetails

class ModuleKindCompatibilityRule : AttributeCompatibilityRule<String> {

    override fun execute(details: CompatibilityCheckDetails<String>): Unit = with(details) {
        if (producerValue == MODULE_KIND_MISSING ||
            consumerValue == MODULE_KIND_MISSING ||
            producerValue in consumerValue?.split('|').orEmpty()) {
            compatible()
        }
    }

}
