plugins {
    alias(libs.plugins.moduleKind)
}

moduleKindConstrains {
    "api" compatibleWith "domain"
    "implementation" compatibleWith "api"
    "app" compatibleWith "implementation"
}
