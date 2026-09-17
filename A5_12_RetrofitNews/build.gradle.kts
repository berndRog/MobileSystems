import java.util.Properties

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
   defaultConfig {
      buildConfigField(
         type = "String",
         name = "NEWS_API_KEY",
         value = "\"$escapedNewsApiKey\"",
      )
   }
}
