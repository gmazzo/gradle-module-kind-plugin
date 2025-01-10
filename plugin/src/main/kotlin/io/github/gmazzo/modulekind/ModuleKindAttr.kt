@file:JvmName("ModuleKindAttr")
package io.github.gmazzo.modulekind

import org.gradle.api.attributes.Attribute

val MODULE_KIND_ATTRIBUTE: Attribute<String> = Attribute.of("io.github.gmazzo.modulekind", String::class.java)
