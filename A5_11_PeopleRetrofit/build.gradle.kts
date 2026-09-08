plugins {
}

android {
   // BuildConfig.DEBUG is used to enable HTTP logging only in debug builds.
   buildFeatures {
      buildConfig = true
   }
}

dependencies {
   // Coil 3 needs its OkHttp network module for image URLs returned by PeopleApi.
   implementation(libs.coil.network.okhttp)
}
