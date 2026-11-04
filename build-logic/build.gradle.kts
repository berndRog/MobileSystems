plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // Android Gradle Plugin: https://developer.android.com/build/releases/gradle-plugin
    implementation("com.android.tools.build:gradle:${libs.versions.agp.get()}")
    // Compose compiler: https://developer.android.com/develop/ui/compose/compiler
    implementation("org.jetbrains.kotlin:compose-compiler-gradle-plugin:${libs.versions.kotlin.get()}")
    // Kotlin serialization: https://kotlinlang.org/docs/serialization.html
    implementation("org.jetbrains.kotlin:kotlin-serialization:${libs.versions.kotlin.get()}")
    // Kotlin Symbol Processing: https://kotlinlang.org/docs/ksp-overview.html
    implementation("com.google.devtools.ksp:symbol-processing-gradle-plugin:${libs.versions.ksp.get()}")
}
