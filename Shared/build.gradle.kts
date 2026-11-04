plugins {
   id("mobile.android.library")
}

dependencies {
   // Compose Activity integration: https://developer.android.com/jetpack/androidx/releases/activity
   implementation(libs.androidx.activity.compose)

   // Navigation 3: https://developer.android.com/jetpack/androidx/releases/navigation3
   implementation(libs.androidx.navigation3.runtime)
   implementation(libs.androidx.navigation3.ui)
   implementation(libs.androidx.lifecycle.viewmodel.navigation3)

   // Room 3 and bundled SQLite: https://developer.android.com/jetpack/androidx/releases/room3
   implementation(libs.androidx.room3.runtime)
   implementation(libs.androidx.sqlite.bundled)
   ksp(libs.androidx.room3.compiler)

   // Koin dependency injection: https://insert-koin.io/docs/reference/koin-android/start/
   implementation(libs.koin.core)
   implementation(libs.koin.android)
   implementation(libs.koin.androidx.compose)

   // Coil image loading: https://coil-kt.github.io/coil/
   implementation(libs.coil.compose)
   implementation(libs.coil.network.okhttp)

   // Retrofit, JSON converters, and HTTP logging: https://square.github.io/retrofit/
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

   // Mockito: https://site.mockito.org/
   androidTestImplementation(libs.mockito.core)
   androidTestImplementation(libs.mockito.android)
   androidTestImplementation(libs.mockito.kotlin)
}
