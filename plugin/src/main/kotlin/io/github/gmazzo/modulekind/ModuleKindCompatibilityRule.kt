package io.github.gmazzo.modulekind

import org.gradle.api.attributes.AttributeCompatibilityRule
import org.gradle.api.attributes.CompatibilityCheckDetails
import org.gradle.api.provider.Provider
import javax.inject.Inject

class ModuleKindCompatibilityRule @Inject constructor(
    constrains: Provider<Map<String, Set<String>>>,
) : AttributeCompatibilityRule<String> {

    private val constrains by lazy(constrains::get)

    override fun execute(details: CompatibilityCheckDetails<String>): Unit = with(details) {
        if (producerValue in constrains[consumerValue].orEmpty()) {
            compatible()
        }
    }

}
