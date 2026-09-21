import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.tasks.testing.Test

// Root build script.
//
// Unlike the previous branches, this branch configures every Android module
// centrally. The module build scripts therefore stay deliberately minimal.
plugins {
   alias(libs.plugins.android.application) apply false
   alias(libs.plugins.android.library) apply false
   alias(libs.plugins.kotlin.compose) apply false
   alias(libs.plugins.kotlin.serialization) apply false
   alias(libs.plugins.google.devtools.ksp) apply false
}

// Keep a reference to the root version catalog. This makes the catalog
// available inside the subprojects block below.
val sharedLibs = libs

subprojects {
   // Shared modules are Android Libraries. Every other module is treated as an
   // independently installable Android application.
   val isSharedLibrary = project.name.startsWith("Shared")

   if (isSharedLibrary) {
      pluginManager.apply("com.android.library")
   }
   else {
      pluginManager.apply("com.android.application")
   }

   // Plugins used by both the app and the Shared Android Library.
   pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
   pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
   pluginManager.apply("com.google.devtools.ksp")

   // Native-access warnings are disabled for local JVM tests that use tools
   // such as Robolectric on current Java versions.
   tasks.withType<Test>().configureEach {
      jvmArgs("--enable-native-access=ALL-UNNAMED")
   }

   if (isSharedLibrary) {
      extensions.configure<LibraryExtension> {
         // Derive a stable namespace from the module name. For Shared this
         // produces de.rogallab.mobile.shared.
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
            testInstrumentationRunner =
               "androidx.test.runner.AndroidJUnitRunner"

            // These rules are packaged into the AAR and are used by every app
            // consuming the Shared module.
            consumerProguardFiles("consumer-rules.keep")
         }

         testOptions {
            animationsDisabled = true
            unitTests.isIncludeAndroidResources = true
         }

         compileOptions {
            // Both modules compile source code and bytecode for Java 21.
            sourceCompatibility = JavaVersion.VERSION_21
            targetCompatibility = JavaVersion.VERSION_21
         }

         buildFeatures {
            // Shared may contain reusable Compose UI components.
            compose = true
         }
      }
   }
   else {
      extensions.configure<ApplicationExtension> {
         // All course examples deliberately use the same source namespace.
         namespace = "de.rogallab.mobile"
         compileSdk = 37

         defaultConfig {
            // Derive a unique application ID from the module name. This lets
            // several course examples remain installed at the same time.
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

            testInstrumentationRunner =
               "androidx.test.runner.AndroidJUnitRunner"
         }

         testOptions {
            animationsDisabled = true
            unitTests.isIncludeAndroidResources = true
         }

         buildTypes {
            release {
               // Course examples remain easy to inspect and debug.
               isMinifyEnabled = false

               // Every application uses the central R8 rules file from the
               // root project.
               proguardFiles(
                  getDefaultProguardFile("proguard-android-optimize.txt"),
                  rootProject.file("proguard-rules.pro")
               )
            }
         }

         compileOptions {
            // Both modules compile source code and bytecode for Java 21.
            sourceCompatibility = JavaVersion.VERSION_21
            targetCompatibility = JavaVersion.VERSION_21
         }

         buildFeatures {
            compose = true

            // Later examples can add module-specific BuildConfig fields.
            buildConfig = true
         }
      }
   }

   // All course dependencies are declared once for every Android subproject.
   // Version numbers and aliases remain in gradle/libs.versions.toml.
   dependencies {
      // ----------------------------------------------------------------------
      // Local project modules
      // ----------------------------------------------------------------------

      // Application modules can use Shared. The library must never depend on
      // an application module or on itself.
      if (!isSharedLibrary) {
         add("implementation", project(":Shared"))
      }

      // ----------------------------------------------------------------------
      // Kotlin and Android core
      // ----------------------------------------------------------------------
      add("implementation", sharedLibs.androidx.core.ktx)
      add("implementation", sharedLibs.kotlinx.coroutines.core)
      add("implementation", sharedLibs.kotlinx.coroutines.android)
      add("implementation", sharedLibs.kotlinx.datetime)
      add("implementation", sharedLibs.kotlinx.serialization.json)

      // ----------------------------------------------------------------------
      // Activity and Jetpack Compose
      // ----------------------------------------------------------------------

      // The BOM selects mutually compatible versions for Compose libraries.
      val composeBom = platform(sharedLibs.androidx.compose.bom)
      add("implementation", composeBom)
      add("testImplementation", composeBom)
      add("androidTestImplementation", composeBom)

      add("implementation", sharedLibs.androidx.activity.compose)
      add("implementation", sharedLibs.androidx.compose.foundation.layout)
      add("implementation", sharedLibs.androidx.ui)
      add("implementation", sharedLibs.androidx.ui.graphics)
      add("implementation", sharedLibs.androidx.ui.tooling.preview)
      add("implementation", sharedLibs.androidx.animation)
      add("implementation", sharedLibs.androidx.material3)
      add("implementation", sharedLibs.androidx.material.icons.extended)

      // ----------------------------------------------------------------------
      // Lifecycle, ViewModel, and Navigation 3
      // ----------------------------------------------------------------------
      add("implementation", sharedLibs.androidx.lifecycle.runtime.ktx)
      add("implementation", sharedLibs.androidx.lifecycle.runtime.compose)
      add("implementation", sharedLibs.androidx.lifecycle.viewmodel.compose)
      add("implementation", sharedLibs.androidx.lifecycle.viewmodel.navigation3)
      add("implementation", sharedLibs.androidx.navigation3.runtime)
      add("implementation", sharedLibs.androidx.navigation3.ui)

      // ----------------------------------------------------------------------
      // Room 3 and bundled SQLite
      // ----------------------------------------------------------------------
      add("implementation", sharedLibs.androidx.room3.runtime)
      add("implementation", sharedLibs.androidx.sqlite.bundled)
      add("ksp", sharedLibs.androidx.room3.compiler)

      // ----------------------------------------------------------------------
      // Images and media
      // ----------------------------------------------------------------------
      add("implementation", sharedLibs.coil.compose)
      add("implementation", sharedLibs.coil.network.okhttp)
      add("implementation", sharedLibs.androidx.media3.exoplayer)

      // ----------------------------------------------------------------------
      // Dependency injection with Koin
      // ----------------------------------------------------------------------
      add("implementation", sharedLibs.koin.core)
      add("implementation", sharedLibs.koin.android)
      add("implementation", sharedLibs.koin.androidx.compose)

      // ----------------------------------------------------------------------
      // Networking with Retrofit
      // ----------------------------------------------------------------------
      add("implementation", sharedLibs.gson.json)
      add("implementation", sharedLibs.retrofit2.core)
      add("implementation", sharedLibs.retrofit2.gson)
      add("implementation", sharedLibs.retrofit2.kotlinx.serialization)
      add("implementation", sharedLibs.retrofit2.logging)

      // ----------------------------------------------------------------------
      // Google Play Services
      // ----------------------------------------------------------------------
      add("implementation", sharedLibs.gplay.location)

      // ----------------------------------------------------------------------
      // Local JVM tests
      // ----------------------------------------------------------------------
      add("testImplementation", sharedLibs.junit)
      add("testImplementation", sharedLibs.androidx.test.core)
      add("testImplementation", sharedLibs.androidx.test.core.ktx)
      add("testImplementation", sharedLibs.koin.test)
      add("testImplementation", sharedLibs.koin.test.junit4)
      add("testImplementation", sharedLibs.kotlinx.coroutines.test)
      add("testImplementation", sharedLibs.turbine.test)
      add("testImplementation", sharedLibs.robolectric.test)

      // ----------------------------------------------------------------------
      // Instrumented Android and Compose tests
      // ----------------------------------------------------------------------
      add("androidTestImplementation", sharedLibs.kotlinx.coroutines.test)
      add("androidTestImplementation", sharedLibs.androidx.test.core)
      add("androidTestImplementation", sharedLibs.androidx.test.core.ktx)
      add("androidTestImplementation", sharedLibs.androidx.test.ext.junit)
      add("androidTestImplementation", sharedLibs.androidx.test.ext.junit.ktx)
      add("androidTestImplementation", sharedLibs.androidx.test.ext.truth)
      add("androidTestImplementation", sharedLibs.androidx.test.runner)
      add("androidTestImplementation", sharedLibs.androidx.ui.test.junit4)
      add("androidTestImplementation", sharedLibs.androidx.test.espresso.core)
      add("androidTestImplementation", sharedLibs.koin.test)
      add("androidTestImplementation", sharedLibs.koin.test.junit4)
      add("androidTestImplementation", sharedLibs.koin.androidx.compose)
      add("androidTestImplementation", sharedLibs.mockito.core)
      add("androidTestImplementation", sharedLibs.mockito.android)
      add("androidTestImplementation", sharedLibs.mockito.kotlin)

      // ----------------------------------------------------------------------
      // Debug-only tooling
      // ----------------------------------------------------------------------
      add("debugImplementation", sharedLibs.androidx.ui.tooling)
      add("debugImplementation", sharedLibs.androidx.ui.test.manifest)
   }
}
