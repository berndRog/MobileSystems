import java.util.Properties

plugins {
}

val localProperties = Properties().apply {
   val localPropertiesFile = rootProject.file("local.properties")
   if (localPropertiesFile.exists()) {
      localPropertiesFile.inputStream().use { inputStream ->
         load(inputStream)
      }
   }
}

val newsApiKey = providers.gradleProperty("NEWS_API_KEY").orNull
   ?: localProperties.getProperty("NEWS_API_KEY")
   ?: ""
val escapedNewsApiKey = newsApiKey
   .replace("\\", "\\\\")
   .replace("\"", "\\\"")

android {
   buildFeatures {
      buildConfig = true
   }
   defaultConfig {
      buildConfigField(
         type = "String",
         name = "NEWS_API_KEY",
         value = "\"$escapedNewsApiKey\"",
      )
   }
}

dependencies {
   // Room 3 uses a SQLiteDriver; the bundled driver keeps SQLite consistent.
   implementation(libs.androidx.sqlite.bundled)

   // Coil 3 needs an explicit network module for http/https image URLs.
   implementation(libs.coil.network.okhttp)
}
