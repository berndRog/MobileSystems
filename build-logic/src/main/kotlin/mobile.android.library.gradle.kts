import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.Test

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

extensions.configure<LibraryExtension>("android") {
    namespace = "de.rogallab.mobile.shared"
    compileSdk {
        version = release(37) { minorApiLevel = 2 }
    }
    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.keep")
    }
    testOptions {
        animationsDisabled = true
        unitTests.isIncludeAndroidResources = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
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
    implementation(catalog.findLibrary("androidx-animation").get())
    implementation(catalog.findLibrary("androidx-material3").get())
    implementation(catalog.findLibrary("androidx-material-icons-extended").get())
    debugImplementation(catalog.findLibrary("androidx-ui-tooling").get())
    debugImplementation(catalog.findLibrary("androidx-ui-test-manifest").get())

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
