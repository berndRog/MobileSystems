import java.util.Properties

// Most Android configuration, plugins, and dependencies are managed centrally
// in the build.gradle.kts file of the root project. This module only adds its
// News API key to the generated BuildConfig class.
val localProperties = Properties().apply {
   val localPropertiesFile = rootProject.file("local.properties")
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

android {
   defaultConfig {
      buildConfigField(
         type = "String",
         name = "NEWS_API_KEY",
         value = "\"$escapedNewsApiKey\"",
      )
   }
}
