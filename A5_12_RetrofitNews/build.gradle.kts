import java.util.Properties

plugins {
   alias(libs.plugins.android.application) apply false
   alias(libs.plugins.android.library) apply false
   alias(libs.plugins.kotlin.compose) apply false
   alias(libs.plugins.kotlin.serialization) apply false
   alias(libs.plugins.google.devtools.ksp) apply false
}

apply(from = "../gradle/android-common.gradle")

// The shared Gradle script supplies the Android configuration and dependencies.
// This application also adds its News API key to the generated BuildConfig class.
val localProperties = Properties().apply {
   val localPropertiesFile = rootProject.file("../local.properties")
   if (localPropertiesFile.exists()) {
      localPropertiesFile.inputStream().use { inputStream ->
         load(inputStream)
      }
   }
}

// A Gradle property takes precedence over the developer-local fallback.
val newsApiKey = providers.gradleProperty("NEWS_API_KEY").orNull
   ?: localProperties.getProperty("NEWS_API_KEY")
   ?: ""

// Escape characters that would otherwise break the generated Kotlin string.
val escapedNewsApiKey = newsApiKey
   .replace("\\", "\\\\")
   .replace("\"", "\\\"")

extensions.configure<com.android.build.api.dsl.ApplicationExtension>("android") {
   defaultConfig {
      buildConfigField(
         type = "String",
         name = "NEWS_API_KEY",
         value = "\"$escapedNewsApiKey\"",
      )
   }
}
