// Open this directory as a separate Gradle project; Shared is included by settings.gradle.kts.
plugins {
   id("mobile.android.application")
}

dependencies {
   // Shared Android library: ../Shared/build.gradle.kts
   implementation(project(":Shared"))

   // Material icons: https://developer.android.com/jetpack/androidx/releases/compose-material
   implementation(libs.androidx.material.icons.extended)

   // Coil image loading: https://coil-kt.github.io/coil/
   implementation(libs.coil.compose)
   implementation(libs.coil.network.okhttp)
}
