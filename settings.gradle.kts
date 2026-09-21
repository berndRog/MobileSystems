// Central plugin repository configuration for the complete Gradle build.
pluginManagement {
   repositories {
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

// Automatically provisions a matching Java toolchain when necessary.
plugins {
   id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// All modules resolve their external libraries from the same repositories.
dependencyResolutionManagement {

   // Module-specific repository declarations are rejected deliberately.
   repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

   repositories {
      google()
      mavenCentral()
   }
}

// Name of the complete course project.
rootProject.name = "MobileSystems"

// Independently runnable Android application modules.
include(":A2_01_Count")
include(":A2_02_Layout")
include(":A2_03_TextField")
include(":A2_04_LazyColumn")
include(":A3_01_Material")
include(":A3_02_EffectHandling")
include(":A3_03_Navigation")
include(":A3_04_SwipeGestures")
include(":A3_05_SwipeDeleteUndo")
include(":A4_01_ImagePicker")
include(":A5_01_PeopleRoom3")
include(":A5_02_UsedCarsRoom3")
include(":A5_10_PeopleRetrofit")
include(":A5_11_PeopleImagesRetrofit")
include(":A5_12_RetrofitNews")
include(":A5_13_UsedCars")

// Reusable Android Library used by every application module.
include(":Shared")
