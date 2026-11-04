// Open this directory as a separate Gradle project; Shared is included by settings.gradle.kts.
plugins {
   id("mobile.android.application")
}

dependencies {
   // Shared Android library: ../Shared/build.gradle.kts
   implementation(project(":Shared"))
}
