import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.Test
import java.util.Locale

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

extensions.configure<ApplicationExtension>("android") {
    namespace = "de.rogallab.mobile"
    compileSdk {
        version = release(37) { minorApiLevel = 2 }
    }
    defaultConfig {
        minSdk = 26
        applicationId = "de.rogallab.mobile.${project.name.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")}" 
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    testOptions {
        animationsDisabled = true
        unitTests.isIncludeAndroidResources = true
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                rootProject.file("../proguard-rules.pro"),
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
dependencies {
    // Compose BOM and UI: https://developer.android.com/develop/ui/compose/bom
    implementation(platform(catalog.findLibrary("androidx-compose-bom").get()))
    testImplementation(platform(catalog.findLibrary("androidx-compose-bom").get()))
    androidTestImplementation(platform(catalog.findLibrary("androidx-compose-bom").get()))
    implementation(catalog.findLibrary("androidx-compose-foundation-layout").get())
    implementation(catalog.findLibrary("androidx-ui").get())
    implementation(catalog.findLibrary("androidx-ui-graphics").get())
    implementation(catalog.findLibrary("androidx-ui-tooling-preview").get())
    implementation(catalog.findLibrary("androidx-material3").get())
    debugImplementation(catalog.findLibrary("androidx-ui-tooling").get())
    debugImplementation(catalog.findLibrary("androidx-ui-test-manifest").get())

    // Activity: https://developer.android.com/jetpack/androidx/releases/activity
    implementation(catalog.findLibrary("androidx-activity-compose").get())
    // Android core: https://developer.android.com/jetpack/androidx/releases/core
    implementation(catalog.findLibrary("androidx-core-ktx").get())

    // Lifecycle: https://developer.android.com/jetpack/androidx/releases/lifecycle
    implementation(catalog.findLibrary("androidx-lifecycle-runtime-ktx").get())
    implementation(catalog.findLibrary("androidx-lifecycle-runtime-compose").get())
    implementation(catalog.findLibrary("androidx-lifecycle-viewmodel-compose").get())
    // Kotlin coroutines: https://kotlinlang.org/docs/coroutines-overview.html
    implementation(catalog.findLibrary("kotlinx-coroutines-core").get())
    implementation(catalog.findLibrary("kotlinx-coroutines-android").get())

    // Local and instrumented tests: https://developer.android.com/training/testing
    testImplementation(catalog.findLibrary("junit").get())
    testImplementation(catalog.findLibrary("androidx-test-core").get())
    testImplementation(catalog.findLibrary("androidx-test-core-ktx").get())
    testImplementation(catalog.findLibrary("kotlinx-coroutines-test").get())
    testImplementation(catalog.findLibrary("turbine-test").get())
    testImplementation(catalog.findLibrary("robolectric-test").get())
    androidTestImplementation(catalog.findLibrary("kotlinx-coroutines-test").get())
    androidTestImplementation(catalog.findLibrary("androidx-test-core").get())
    androidTestImplementation(catalog.findLibrary("androidx-test-core-ktx").get())
    androidTestImplementation(catalog.findLibrary("androidx-test-ext-junit").get())
    androidTestImplementation(catalog.findLibrary("androidx-test-ext-junit-ktx").get())
    androidTestImplementation(catalog.findLibrary("androidx-test-ext-truth").get())
    androidTestImplementation(catalog.findLibrary("androidx-test-runner").get())
    androidTestImplementation(catalog.findLibrary("androidx-ui-test-junit4").get())
    androidTestImplementation(catalog.findLibrary("androidx-test-espresso-core").get())
}
