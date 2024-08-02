plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktorfit)
    alias(libs.plugins.sekret)
}

kotlin {
    jvm()

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    macosX64()
    macosArm64()

    js(IR) {
        browser()
        nodejs()
        binaries.executable()
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.sekret)
            implementation(libs.serialization)
            implementation(libs.tooling)
            api(libs.immutable)
            api(libs.ktorfit)

            implementation(project(":core"))
        }
    }
}