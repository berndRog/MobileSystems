// Zentrale Plug-in-Verwaltung für das gesamte Gradle-Projekt.
pluginManagement {
   repositories {

      // Android-, Google- und AndroidX-Plug-ins.
      google {
         content {
            includeGroupByRegex("com\\.android.*")
            includeGroupByRegex("com\\.google.*")
            includeGroupByRegex("androidx.*")
         }
      }

      mavenCentral()
      gradlePluginPortal()
   }
}

// Automatische Bereitstellung einer passenden Java-Toolchain.
plugins {
   id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// Zentrale Repository-Konfiguration für alle Bibliotheken
// und alle Beispielmodule.
dependencyResolutionManagement {

   // Einzelne Module dürfen keine eigenen Repositories definieren.
   repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

   repositories {
      google()
      mavenCentral()
   }
}

// Name des gesamten Vorlesungsprojekts.
rootProject.name = "MobileSystems"

// Eigenständig startbare Android-Beispielmodule.
include(":A2_01_Count")
include(":A2_02_Layout")
include(":A2_03_TextField")
include(":A2_04_LazyColumn")
include(":A2_05_LazyRow_Images")
include(":A3_01_Material")
include(":A3_02_EffectHandling")
include(":A3_03_Navigation")
include(":A3_04_ImagePicker")
include(":A3_05_SwipeGestures")
include(":Shared")
