// Open this directory as a separate Gradle project; Shared is included by settings.gradle.kts.
plugins {
   id("mobile.android.application")
}

dependencies {
   // Shared Android library: ../Shared/build.gradle.kts
   implementation(project(":Shared"))

   // Material icons: https://developer.android.com/jetpack/androidx/releases/compose-material
   implementation(libs.androidx.material.icons.extended)

   // Room 3 and bundled SQLite: https://developer.android.com/jetpack/androidx/releases/room3
   implementation(libs.androidx.room3.runtime)
   implementation(libs.androidx.sqlite.bundled)

   // Koin dependency injection: https://insert-koin.io/docs/reference/koin-android/start/
   implementation(libs.koin.core)
   implementation(libs.koin.android)
   implementation(libs.koin.androidx.compose)

   // Koin testing: https://insert-koin.io/docs/reference/koin-test/testing/
   testImplementation(libs.koin.test)
   testImplementation(libs.koin.test.junit4)
   androidTestImplementation(libs.koin.test)
   androidTestImplementation(libs.koin.test.junit4)
   androidTestImplementation(libs.koin.androidx.compose)
}
