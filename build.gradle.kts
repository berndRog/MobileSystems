import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion

// Top-level build file.
// Hier werden Konfigurationen verwaltet, die für alle Beispielmodule gelten.
plugins {
   alias(libs.plugins.android.application) apply false

   // Ab AGP 9 wird Kotlin direkt durch das Android-Gradle-Plug-in unterstützt.
   // alias(libs.plugins.kotlin.android) apply false

   alias(libs.plugins.kotlin.compose) apply false
   alias(libs.plugins.google.devtools.ksp) apply false
   alias(libs.plugins.kotlin.serialization) apply false
   alias(libs.plugins.android.library) apply false
}

// Der Version Catalog gehört zum Root-Projekt.
// Er wird hier gespeichert, damit er auch innerhalb der zentralen
// Konfiguration aller Subprojekte verwendet werden kann.
val sharedLibs = libs

subprojects {

   // Shared-Module sind Android-Bibliotheken.
   // (Historisch hießen sie Shared_01, Shared_02, ...)
   val isSharedLibrary = project.name.startsWith("Shared")

   if (isSharedLibrary) {
      pluginManager.apply("com.android.library")
   }
   else {
      pluginManager.apply("com.android.application")
   }

   // Gemeinsame Plug-ins für Apps und Android-Bibliotheken.
   pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
   pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
   pluginManager.apply("com.google.devtools.ksp")

   if (isSharedLibrary) {

      extensions.configure<LibraryExtension> {

         // Jede Android-Bibliothek erhält einen eigenen Namespace.
         // Aus Shared_01 wird beispielsweise de.rogallab.mobile.shared01.
         val cleanLibraryName = project.name
            .lowercase()
            .replace(
               regex = Regex("[^a-z0-9]"),
               replacement = ""
            )

         namespace = "de.rogallab.mobile.$cleanLibraryName"

         compileSdk = 37

         defaultConfig {

            minSdk = 26

            // Für instrumentierte Tests innerhalb der Android-Bibliothek.
            testInstrumentationRunner =
               "androidx.test.runner.AndroidJUnitRunner"
         }

         testOptions {

            // Animationen während instrumentierter Tests deaktivieren.
            animationsDisabled = true

            // Android-Ressourcen auch in lokalen JVM-Tests bereitstellen.
            // Dies wird insbesondere für Robolectric benötigt.
            unitTests.isIncludeAndroidResources = true
         }

         compileOptions {

            // Java-Quellcode wird mit Java 21 kompiliert.
            sourceCompatibility = JavaVersion.VERSION_21

            // Der erzeugte Java- und Kotlin-Bytecode verwendet JVM 21.
            targetCompatibility = JavaVersion.VERSION_21
         }

         buildFeatures {

            // Jetpack Compose auch für gemeinsame UI-Komponenten aktivieren.
            compose = true
         }
      }
   }
   else {

      extensions.configure<ApplicationExtension> {

         // Alle Vorlesungsbeispiele verwenden bewusst denselben Namespace.
         namespace = "de.rogallab.mobile"

         compileSdk = 37

         defaultConfig {

            // Aus dem Modulnamen wird eine eindeutige Application-ID erzeugt.
            // Dadurch können mehrere Beispiel-Apps gleichzeitig installiert werden.
            val cleanProjectName = project.name
               .lowercase()
               .replace(
                  regex = Regex("[^a-z0-9]"),
                  replacement = ""
               )

            applicationId = "de.rogallab.mobile.$cleanProjectName"

            minSdk = 26
            targetSdk = 37

            versionCode = 1
            versionName = "1.0"

            // Standard-TestRunner für alle Instrumentation Tests.
            testInstrumentationRunner =
               "androidx.test.runner.AndroidJUnitRunner"
         }

         testOptions {

            // Animationen während instrumentierter Tests deaktivieren.
            animationsDisabled = true

            // Android-Ressourcen auch in lokalen JVM-Tests bereitstellen.
            // Dies wird insbesondere für Robolectric benötigt.
            unitTests.isIncludeAndroidResources = true
         }

         buildTypes {

            release {

               // Für die Vorlesungsbeispiele wird keine Code-Minifizierung verwendet.
               isMinifyEnabled = false

               // Alle Beispiel-Apps verwenden die zentrale ProGuard-Datei
               // aus dem Root-Projekt.
               proguardFiles(
                  getDefaultProguardFile("proguard-android-optimize.txt"),
                  rootProject.file("proguard-rules.pro")
               )
            }
         }

         compileOptions {

            // Java-Quellcode wird mit Java 21 kompiliert.
            sourceCompatibility = JavaVersion.VERSION_21

            // Der erzeugte Java- und Kotlin-Bytecode verwendet JVM 21.
            targetCompatibility = JavaVersion.VERSION_21
         }

         buildFeatures {

            // Jetpack Compose für alle Beispielmodule aktivieren.
            compose = true
         }
      }
   }

   // Gemeinsame Bibliotheken für alle Beispielmodule.
   //
   // Die Versionen und Aliase werden zentral im Gradle Version Catalog
   // unter gradle/libs.versions.toml verwaltet.
   //
   // Einführung in den Gradle Version Catalog:
   // https://www.youtube.com/watch?v=MWw1jcwPK3Q
   dependencies {

      // -----------------------------------------------------------------------
      // Common local Android-Libraries
      // -----------------------------------------------------------------------

      // Alle Beispiel-Apps verwenden die gemeinsame Bibliothek Shared.
      // Shared selbst erhält keine Abhängigkeit auf sich selbst.
      if (!isSharedLibrary) {
         add("implementation", project(":Shared"))
      }

      // -----------------------------------------------------------------------
      // Kotlin
      // -----------------------------------------------------------------------
      // AndroidX Core Kotlin Extensions
      // https://developer.android.com/jetpack/androidx/releases/core
      add("implementation", sharedLibs.androidx.core.ktx)

      // Kotlin Coroutines
      // https://kotlinlang.org/docs/releases.html
      add("implementation", sharedLibs.kotlinx.coroutines.core)
      add("implementation", sharedLibs.kotlinx.coroutines.android)

      // Kotlin Date and Time
      add("implementation", sharedLibs.kotlinx.datetime)

      // Kotlin Serialization
      add("implementation", sharedLibs.kotlinx.serialization.json)

      // UI: Activity und Compose
      // ------------------------
      // Compose-Unterstützung für Android Activities
      // https://developer.android.com/jetpack/androidx/releases/activity
      add("implementation", sharedLibs.androidx.activity.compose)

      // Compose Foundation Layouts
      add("implementation", sharedLibs.androidx.compose.foundation.layout)

      // Compose Bill of Materials
      //
      // Die BOM sorgt dafür, dass alle Compose-Bibliotheken zueinander
      // kompatible Versionen verwenden.
      //
      // https://developer.android.com/jetpack/compose/bom/bom-mapping
      val composeBom = platform(sharedLibs.androidx.compose.bom)

      add("implementation", composeBom)
      add("testImplementation", composeBom)
      add("androidTestImplementation", composeBom)

      // Compose UI
      add("implementation", sharedLibs.androidx.ui)
      add("implementation", sharedLibs.androidx.ui.graphics)
      add("implementation", sharedLibs.androidx.ui.tooling.preview)

      // Compose Animation
      add("implementation", sharedLibs.androidx.animation)

      // Material Design 3
      add("implementation", sharedLibs.androidx.material3)
      add("implementation", sharedLibs.androidx.material.icons.extended)

      // -----------------------------------------------------------------------
      // UI: Lifecycle
      // -----------------------------------------------------------------------
      // https://developer.android.com/jetpack/androidx/releases/lifecycle

      // ViewModel-Unterstützung für Compose
      add("implementation", sharedLibs.androidx.lifecycle.viewmodel.compose)

      // Lifecycle-Unterstützung für Compose
      add("implementation", sharedLibs.androidx.lifecycle.runtime.compose)

      // ViewModel-Unterstützung für Navigation 3
      add("implementation", sharedLibs.androidx.lifecycle.viewmodel.navigation3)

      // -----------------------------------------------------------------------
      // UI: Navigation
      // -----------------------------------------------------------------------
      // Navigation 2 für Jetpack Compose:
      // add("implementation", sharedLibs.androidx.navigation.compose)

      // Navigation 3
      // https://developer.android.com/jetpack/androidx/releases/navigation3
      add("implementation", sharedLibs.androidx.navigation3.runtime)
      add("implementation", sharedLibs.androidx.navigation3.ui)

      // -----------------------------------------------------------------------
      // Room
      // -----------------------------------------------------------------------
      // Room Runtime und Kotlin-Coroutines-Unterstützung
      add("implementation", sharedLibs.androidx.room3.runtime)

      // Room-Codegenerierung über Kotlin Symbol Processing
      add("ksp", sharedLibs.androidx.room3.compiler)

      // -----------------------------------------------------------------------
      // Image Loading
      // -----------------------------------------------------------------------
      // Coil für Jetpack Compose
      // https://coil-kt.github.io/coil/
      add("implementation", sharedLibs.coil.compose)

      // -----------------------------------------------------------------------
      // Dependency Injection mit Koin
      // -----------------------------------------------------------------------
      // https://insert-koin.io/docs/quickstart/android/

      // Optional bei Verwendung einer Koin BOM:
      // add("implementation", platform(sharedLibs.koin.bom))

      add("implementation", sharedLibs.koin.core)
      add("implementation", sharedLibs.koin.android)
      add("implementation", sharedLibs.koin.androidx.compose)

      // -----------------------------------------------------------------------
      // Netzwerkzugriff mit Retrofit
      // -----------------------------------------------------------------------
      // Gson
      add("implementation", sharedLibs.gson.json)

      // Retrofit
      add("implementation", sharedLibs.retrofit2.core)
      add("implementation", sharedLibs.retrofit2.gson)

      // HTTP-Logging für Retrofit beziehungsweise OkHttp
      add("implementation", sharedLibs.retrofit2.logging)

      // -----------------------------------------------------------------------
      // Google Play Services
      // -----------------------------------------------------------------------
      // Standortdienste
      add("implementation", sharedLibs.gplay.location)

      // -----------------------------------------------------------------------
      // Lokale Unit Tests
      // -----------------------------------------------------------------------
      // JUnit 4
      add("testImplementation", sharedLibs.junit)

      // AndroidX Test Core für lokale Tests
      add("testImplementation", sharedLibs.androidx.test.core)
      add("testImplementation", sharedLibs.androidx.test.core.ktx)

      // Koin Tests
      add("testImplementation", sharedLibs.koin.test)
      add("testImplementation", sharedLibs.koin.test.junit4)

      // Coroutines-, Flow- und StateFlow-Tests
      add("testImplementation", sharedLibs.kotlinx.coroutines.test)
      add("testImplementation", sharedLibs.turbine.test)

      // Robolectric für Android-nahe Tests auf der lokalen JVM
      add("testImplementation", sharedLibs.robolectric.test)

      // -----------------------------------------------------------------------
      // Instrumentierte Android Tests
      // -----------------------------------------------------------------------
      // Coroutines Tests
      add("androidTestImplementation", sharedLibs.kotlinx.coroutines.test)

      // AndroidX Test Core
      add("androidTestImplementation", sharedLibs.androidx.test.core)
      add("androidTestImplementation", sharedLibs.androidx.test.core.ktx)

      // AndroidX JUnit Extensions
      add("androidTestImplementation", sharedLibs.androidx.test.ext.junit)
      add("androidTestImplementation", sharedLibs.androidx.test.ext.junit.ktx)

      // Truth Assertions
      add("androidTestImplementation", sharedLibs.androidx.test.ext.truth)

      // AndroidX Test Runner
      add("androidTestImplementation", sharedLibs.androidx.test.runner)

      // Compose UI Tests
      //
      // Die Compose BOM wurde bereits weiter oben auch für
      // androidTestImplementation eingebunden.
      add("androidTestImplementation", sharedLibs.androidx.ui.test.junit4)

      // Navigation Tests
      // add("androidTestImplementation", sharedLibs.androidx.navigation.testing)

      // Room Tests
      add("androidTestImplementation", sharedLibs.androidx.room3.testing)

      // LiveData- und Architecture-Components-Tests
      // add("androidTestImplementation", sharedLibs.androidx.arch.core.testing)

      // Koin Tests
      add("androidTestImplementation", sharedLibs.koin.test)
      add("androidTestImplementation", sharedLibs.koin.test.junit4)
      add("androidTestImplementation", sharedLibs.koin.androidx.compose)

      // Espresso UI Tests
      add("androidTestImplementation", sharedLibs.androidx.test.espresso.core)

      // Mockito
      add("androidTestImplementation", sharedLibs.mockito.core)
      add("androidTestImplementation", sharedLibs.mockito.android)
      add("androidTestImplementation", sharedLibs.mockito.kotlin)

      // -----------------------------------------------------------------------
      // Debug-Abhängigkeiten
      // -----------------------------------------------------------------------

      // Compose Layout Inspector und UI Preview
      //
      // ui-tooling wird bewusst nur für Debug-Builds eingebunden und
      // nicht in die Release-Anwendung aufgenommen.
      add("debugImplementation", sharedLibs.androidx.ui.tooling)

      // Manifest-Unterstützung für Compose UI Tests
      add("debugImplementation", sharedLibs.androidx.ui.test.manifest)
   }
}

/* ohne libraries

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion

// Top-level build file.
// Hier werden Konfigurationen verwaltet, die für alle Beispielmodule gelten.
plugins {
   alias(libs.plugins.android.application) apply false

   // Ab AGP 9 wird Kotlin direkt durch das Android-Gradle-Plug-in unterstützt.
   // alias(libs.plugins.kotlin.android) apply false

   alias(libs.plugins.kotlin.compose) apply false
   alias(libs.plugins.google.devtools.ksp) apply false
   alias(libs.plugins.kotlin.serialization) apply false
}

// Der Version Catalog gehört zum Root-Projekt.
// Er wird hier gespeichert, damit er auch innerhalb der zentralen
// Konfiguration aller Subprojekte verwendet werden kann.
val sharedLibs = libs

subprojects {

   // Alle Subprojekte sind eigenständig startbare Android-Anwendungen.
   pluginManager.apply("com.android.application")

   // Ab AGP 9 nicht mehr erforderlich:
   // pluginManager.apply("org.jetbrains.kotlin.android")

   pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
   pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
   pluginManager.apply("com.google.devtools.ksp")

   extensions.configure<ApplicationExtension> {

      // Alle Vorlesungsbeispiele verwenden bewusst denselben Namespace.
      namespace = "de.rogallab.mobile"

      compileSdk = 37

      defaultConfig {

         // Aus dem Modulnamen wird eine eindeutige Application-ID erzeugt.
         // Dadurch können mehrere Beispiel-Apps gleichzeitig installiert werden.
         val cleanProjectName = project.name
            .lowercase()
            .replace(
               regex = Regex("[^a-z0-9]"),
               replacement = ""
            )

         applicationId = "de.rogallab.mobile.$cleanProjectName"

         minSdk = 26
         targetSdk = 37

         versionCode = 1
         versionName = "1.0"

         // Standard-TestRunner:
         // testInstrumentationRunner =
         //    "androidx.test.runner.AndroidJUnitRunner"

         // Eigener TestRunner für alle Instrumentation Tests.
         testInstrumentationRunner =
            "de.rogallab.mobile.androidTest.TestRunner"
      }

      testOptions {

         // Animationen während instrumentierter Tests deaktivieren.
         animationsDisabled = true

         // Android-Ressourcen auch in lokalen JVM-Tests bereitstellen.
         // Dies wird insbesondere für Robolectric benötigt.
         unitTests.isIncludeAndroidResources = true
      }

      buildTypes {

         release {

            // Für die Vorlesungsbeispiele wird keine Code-Minifizierung verwendet.
            isMinifyEnabled = false

            // Zentrale
            proguardFiles(
               getDefaultProguardFile("proguard-android-optimize.txt"),
               rootProject.file("proguard-rules.pro")
            )
         }
      }

      compileOptions {

         // Java-Quellcode wird mit Java 17 kompiliert.
         sourceCompatibility = JavaVersion.VERSION_21

         // Der erzeugte Java- und Kotlin-Bytecode verwendet JVM 17.
         targetCompatibility = JavaVersion.VERSION_21
      }

      buildFeatures {

         // Jetpack Compose für alle Beispielmodule aktivieren.
         compose = true
      }
   }

   // Gemeinsame Bibliotheken für alle Beispielmodule.
   //
   // Die Versionen und Aliase werden zentral im Gradle Version Catalog
   // unter gradle/libs.versions.toml verwaltet.
   //
   // Einführung in den Gradle Version Catalog:
   // https://www.youtube.com/watch?v=MWw1jcwPK3Q
   dependencies {

      // -----------------------------------------------------------------------
      // Kotlin
      // -----------------------------------------------------------------------
      // AndroidX Core Kotlin Extensions
      // https://developer.android.com/jetpack/androidx/releases/core
      add("implementation", sharedLibs.androidx.core.ktx)

      // Kotlin Coroutines
      // https://kotlinlang.org/docs/releases.html
      add("implementation", sharedLibs.kotlinx.coroutines.core)
      add("implementation", sharedLibs.kotlinx.coroutines.android)

      // Kotlin Date and Time
      add("implementation", sharedLibs.kotlinx.datetime)

      // Kotlin Serialization
      add("implementation", sharedLibs.kotlinx.serialization.json)

      // -----------------------------------------------------------------------
      // UI: Activity und Compose
      // -----------------------------------------------------------------------
      // Compose-Unterstützung für Android Activities
      // https://developer.android.com/jetpack/androidx/releases/activity
      add("implementation", sharedLibs.androidx.activity.compose)

      // Compose Foundation Layouts
      add("implementation", sharedLibs.androidx.compose.foundation.layout)

      // Compose Bill of Materials
      //
      // Die BOM sorgt dafür, dass alle Compose-Bibliotheken zueinander
      // kompatible Versionen verwenden.
      //
      // https://developer.android.com/jetpack/compose/bom/bom-mapping
      val composeBom = platform(sharedLibs.androidx.compose.bom)

      add("implementation", composeBom)
      add("testImplementation", composeBom)
      add("androidTestImplementation", composeBom)

      // Compose UI
      add("implementation", sharedLibs.androidx.ui)
      add("implementation", sharedLibs.androidx.ui.graphics)
      add("implementation", sharedLibs.androidx.ui.tooling.preview)
      add("implementation", sharedLibs.androidx.ui.text.google.fonts)

      // Compose Animation
      add("implementation", sharedLibs.androidx.animation)

      // Material Design 3
      add("implementation", sharedLibs.androidx.material3)
      add("implementation", sharedLibs.androidx.material.icons.extended)

      // Adaptive Layouts
      add("implementation", sharedLibs.androidx.material3.adaptive)
      add("implementation", sharedLibs.androidx.material3.windowsizeclass)

      // -----------------------------------------------------------------------
      // UI: Lifecycle
      // -----------------------------------------------------------------------
      // https://developer.android.com/jetpack/androidx/releases/lifecycle

      // ViewModel-Unterstützung für Compose
      add("implementation", sharedLibs.androidx.lifecycle.viewmodel.compose)

      // Lifecycle-Unterstützung für Compose
      add("implementation", sharedLibs.androidx.lifecycle.runtime.compose)

      // ViewModel-Unterstützung für Navigation 3
      add("implementation", sharedLibs.androidx.lifecycle.viewmodel.navigation3)

      // -----------------------------------------------------------------------
      // UI: Navigation
      // -----------------------------------------------------------------------
      // Navigation 2 für Jetpack Compose:
      // add("implementation", sharedLibs.androidx.navigation.compose)

      // Navigation 3
      // https://developer.android.com/jetpack/androidx/releases/navigation3
      add("implementation", sharedLibs.androidx.navigation3.runtime)
      add("implementation", sharedLibs.androidx.navigation3.ui)

      // -----------------------------------------------------------------------
      // Room
      // -----------------------------------------------------------------------
      // Room Runtime und Kotlin-Coroutines-Unterstützung
      add("implementation", sharedLibs.androidx.room.ktx)
      add("implementation", sharedLibs.androidx.room.runtime)

      // Room-Codegenerierung über Kotlin Symbol Processing
      add("ksp", sharedLibs.androidx.room.compiler)

      // -----------------------------------------------------------------------
      // Image Loading
      // -----------------------------------------------------------------------
      // Coil für Jetpack Compose
      // https://coil-kt.github.io/coil/
      add("implementation", sharedLibs.coil.compose)

      // -----------------------------------------------------------------------
      // Dependency Injection mit Koin
      // -----------------------------------------------------------------------
      // https://insert-koin.io/docs/quickstart/android/

      // Optional bei Verwendung einer Koin BOM:
      // add("implementation", platform(sharedLibs.koin.bom))

      add("implementation", sharedLibs.koin.core)
      add("implementation", sharedLibs.koin.android)
      add("implementation", sharedLibs.koin.androidx.compose)

      // -----------------------------------------------------------------------
      // Netzwerkzugriff mit Retrofit
      // -----------------------------------------------------------------------
      // Gson
      add("implementation", sharedLibs.gson.json)

      // Retrofit
      add("implementation", sharedLibs.retrofit2.core)
      add("implementation", sharedLibs.retrofit2.gson)

      // HTTP-Logging für Retrofit beziehungsweise OkHttp
      add("implementation", sharedLibs.retrofit2.logging)

      // -----------------------------------------------------------------------
      // Google Play Services
      // -----------------------------------------------------------------------
      // Standortdienste
      add("implementation", sharedLibs.gplay.location)

      // -----------------------------------------------------------------------
      // Lokale Unit Tests
      // -----------------------------------------------------------------------
      // JUnit 4
      add("testImplementation", sharedLibs.junit)

      // AndroidX Test Core für lokale Tests
      add("testImplementation", sharedLibs.androidx.test.core)
      add("testImplementation", sharedLibs.androidx.test.core.ktx)

      // Koin Tests
      add("testImplementation", sharedLibs.koin.test)
      add("testImplementation", sharedLibs.koin.test.junit4)

      // Coroutines-, Flow- und StateFlow-Tests
      add("testImplementation", sharedLibs.kotlinx.coroutines.test)
      add("testImplementation", sharedLibs.turbine.test)

      // Robolectric für Android-nahe Tests auf der lokalen JVM
      add("testImplementation", sharedLibs.robolectric.test)

      // -----------------------------------------------------------------------
      // Instrumentierte Android Tests
      // -----------------------------------------------------------------------
      // Coroutines Tests
      add("androidTestImplementation", sharedLibs.kotlinx.coroutines.test)

      // AndroidX Test Core
      add("androidTestImplementation", sharedLibs.androidx.test.core)
      add("androidTestImplementation", sharedLibs.androidx.test.core.ktx)

      // AndroidX JUnit Extensions
      add("androidTestImplementation", sharedLibs.androidx.test.ext.junit)
      add("androidTestImplementation", sharedLibs.androidx.test.ext.junit.ktx)

      // Truth Assertions
      add("androidTestImplementation", sharedLibs.androidx.test.ext.truth)

      // AndroidX Test Runner
      add("androidTestImplementation", sharedLibs.androidx.test.runner)

      // Compose UI Tests
      //
      // Die Compose BOM wurde bereits weiter oben auch für
      // androidTestImplementation eingebunden.
      add("androidTestImplementation", sharedLibs.androidx.ui.test.junit4)

      // Navigation Tests
      // add("androidTestImplementation", sharedLibs.androidx.navigation.testing)

      // Room Tests
      add("androidTestImplementation", sharedLibs.androidx.room.testing)

      // LiveData- und Architecture-Components-Tests
      add("androidTestImplementation", sharedLibs.androidx.arch.core.testing)

      // Koin Tests
      add("androidTestImplementation", sharedLibs.koin.test)
      add("androidTestImplementation", sharedLibs.koin.test.junit4)
      add("androidTestImplementation", sharedLibs.koin.android.test)
      add("androidTestImplementation", sharedLibs.koin.androidx.compose)

      // Espresso UI Tests
      add("androidTestImplementation", sharedLibs.androidx.test.espresso.core)

      // Mockito
      add("androidTestImplementation", sharedLibs.mockito.core)
      add("androidTestImplementation", sharedLibs.mockito.android)
      add("androidTestImplementation", sharedLibs.mockito.kotlin)

      // -----------------------------------------------------------------------
      // Debug-Abhängigkeiten
      // -----------------------------------------------------------------------

      // Compose Layout Inspector und UI Preview
      //
      // ui-tooling wird bewusst nur für Debug-Builds eingebunden und
      // nicht in die Release-Anwendung aufgenommen.
      add("debugImplementation", sharedLibs.androidx.ui.tooling)

      // Manifest-Unterstützung für Compose UI Tests
      add("debugImplementation", sharedLibs.androidx.ui.test.manifest)
   }
}

 */