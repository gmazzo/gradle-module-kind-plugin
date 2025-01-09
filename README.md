![GitHub](https://img.shields.io/github/license/gmazzo/gradle-module-kind-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.gmazzo.modulekind)](https://plugins.gradle.org/plugin/io.github.gmazzo.modulekind)
[![Build Status](https://github.com/gmazzo/gradle-module-kind-plugin/actions/workflows/build.yaml/badge.svg)](https://github.com/gmazzo/gradle-module-kind-plugin/actions/workflows/build.yaml)
[![Coverage](https://codecov.io/gh/gmazzo/gradle-module-kind-plugin/branch/main/graph/badge.svg?token=D5cDiPWvcS)](https://codecov.io/gh/gmazzo/gradle-module-kind-plugin)
[![Users](https://img.shields.io/badge/users_by-Sourcegraph-purple)](https://sourcegraph.com/search?q=content:io.github.gmazzo.modulekind+-repo:github.com/gmazzo/gradle-module-kind-plugin)

# gradle-module-kind-plugin
A Gradle plugin to constraints a multi-module build dependency graph.

# Usage
Apply the plugin:
```kotlin
plugins {
    java
    id("io.github.gmazzo.modulekind") version "<latest>" 
}
```
