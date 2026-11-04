// Open this directory as a separate Gradle project; Shared is included by settings.gradle.kts.
plugins {
   id("mobile.android.application")
}

dependencies {
   // Shared Android library: ../Shared/build.gradle.kts
   implementation(project(":Shared"))

   // Compose animation: https://developer.android.com/jetpack/androidx/releases/compose-animation
   implementation(libs.androidx.animation)

   // Material icons: https://developer.android.com/jetpack/androidx/releases/compose-material
   implementation(libs.androidx.material.icons.extended)

   // Navigation 3: https://developer.android.com/jetpack/androidx/releases/navigation3
   implementation(libs.androidx.navigation3.runtime)
   implementation(libs.androidx.navigation3.ui)
   implementation(libs.androidx.lifecycle.viewmodel.navigation3)

   // Koin dependency injection: https://insert-koin.io/docs/reference/koin-android/start/
   implementation(libs.koin.core)
   implementation(libs.koin.android)
   implementation(libs.koin.androidx.compose)

   // Coil image loading: https://coil-kt.github.io/coil/
   implementation(libs.coil.compose)
   implementation(libs.coil.network.okhttp)

   // Retrofit and HTTP logging: https://square.github.io/retrofit/
   implementation(libs.retrofit2.core)
   implementation(libs.retrofit2.gson)
   implementation(libs.retrofit2.kotlinx.serialization)
   implementation(libs.retrofit2.logging)
   implementation(libs.gson.json)

   // Kotlin serialization: https://kotlinlang.org/docs/serialization.html
   implementation(libs.kotlinx.serialization.json)

   // Kotlin date and time: https://github.com/Kotlin/kotlinx-datetime
   implementation(libs.kotlinx.datetime)

   // Koin testing: https://insert-koin.io/docs/reference/koin-test/testing/
   testImplementation(libs.koin.test)
   testImplementation(libs.koin.test.junit4)
   androidTestImplementation(libs.koin.test)
   androidTestImplementation(libs.koin.test.junit4)
   androidTestImplementation(libs.koin.androidx.compose)
}
