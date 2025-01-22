enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
        google()
    }
}

rootProject.name = "gradle-module-kind-plugin"

includeBuild("plugin")
include(
    "demo:domain-users",
    "demo:domain-items",
    "demo:feature-listing-api",
    "demo:feature-listing-impl",
    "demo:feature-login-api",
    "demo:feature-login-impl",
    "demo:app",
    "demo-groovy",
)
