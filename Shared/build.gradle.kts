plugins {
   // leer lassen (wird zentral im Root-Gradle gesetzt)
}

dependencies {
   // Room3
   implementation(libs.androidx.room3.runtime)
   implementation(libs.androidx.sqlite.bundled)
   ksp(libs.androidx.room3.compiler)

   // Coil
   implementation(libs.coil.compose)

   // SQLDriver für Android (AndroidSQLiteDriver)
   implementation(libs.androidx.sqlite.framework)

   // optional Tests
   testImplementation(libs.androidx.room3.testing)
}