// Open this directory as a separate Gradle project; Shared is included by settings.gradle.kts.
plugins {
   id("mobile.android.application")
}

dependencies {
   // Shared Android library: ../Shared/build.gradle.kts
   implementation(project(":Shared"))
   implementation(libs.androidx.camera.core)
   implementation(libs.androidx.camera.camera2)
   implementation(libs.androidx.camera.lifecycle)
   implementation(libs.androidx.camera.video)
   implementation(libs.androidx.camera.compose)

   // Material icons: https://developer.android.com/jetpack/androidx/releases/compose-material
   implementation(libs.androidx.material.icons.extended)
   implementation(libs.androidx.room.ktx)

   // Koin dependency injection: https://insert-koin.io/docs/reference/koin-android/start/
   implementation(libs.koin.core)
   implementation(libs.koin.android)
   implementation(libs.koin.androidx.compose)

   // Coil image loading: https://coil-kt.github.io/coil/
   implementation(libs.coil.compose)
   implementation(libs.coil.network.okhttp)
}
