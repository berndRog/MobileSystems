import java.util.Properties

// Open this directory as a separate Gradle project; Shared is included by settings.gradle.kts.
plugins {
   id("mobile.android.application")
}

dependencies {
   // Shared Android library: ../Shared/build.gradle.kts
   implementation(project(":Shared"))

   // Material icons: https://developer.android.com/jetpack/androidx/releases/compose-material
   implementation(libs.androidx.material.icons.extended)

   // Navigation 3: https://developer.android.com/jetpack/androidx/releases/navigation3
   implementation(libs.androidx.navigation3.runtime)
   implementation(libs.androidx.navigation3.ui)
   implementation(libs.androidx.lifecycle.viewmodel.navigation3)

   // Room 3 and bundled SQLite: https://developer.android.com/jetpack/androidx/releases/room3
   implementation(libs.androidx.room3.runtime)
   implementation(libs.androidx.sqlite.bundled)

   // Room compiler (KSP): https://developer.android.com/jetpack/androidx/releases/room3
   ksp(libs.androidx.room3.compiler)

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

   // Koin testing: https://insert-koin.io/docs/reference/koin-test/testing/
   testImplementation(libs.koin.test)
   testImplementation(libs.koin.test.junit4)
   androidTestImplementation(libs.koin.test)
   androidTestImplementation(libs.koin.test.junit4)
   androidTestImplementation(libs.koin.androidx.compose)
}

// The convention plugin supplies the common Android configuration and dependencies.
// This application also adds its News API key to the generated BuildConfig class.
val localProperties = Properties().apply {
   // Keep the repository-wide fallback and let an app-local value override it.
   listOf(rootProject.file("../local.properties"), rootProject.file("local.properties"))
      .filter { it.isFile }
      .forEach { file ->
         file.inputStream().use { inputStream -> load(inputStream) }
      }
}

// A Gradle property takes precedence over the app-local fallback.
val newsApiKey = providers.gradleProperty("NEWS_API_KEY").orNull
   ?: localProperties.getProperty("NEWS_API_KEY")
   ?: ""

// Escape characters that would otherwise break the generated Kotlin string.
val escapedNewsApiKey = newsApiKey
   .replace("\\", "\\\\")
   .replace("\"", "\\\"")
   .replace("\n", "\\n")
   .replace("\r", "\\r")

extensions.configure<com.android.build.api.dsl.ApplicationExtension>("android") {
   defaultConfig {
      buildConfigField(
         type = "String",
         name = "NEWS_API_KEY",
         value = "\"$escapedNewsApiKey\"",
      )
   }
}
