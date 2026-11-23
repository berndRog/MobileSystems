package de.rogallab.mobile.data.local.database

import androidx.room3.migration.Migration
import androidx.sqlite.execSQL

// Adds the JSON column that stores the local vehicle image paths.
val migration1To2 = Migration(
   startVersion = 1,
   endVersion = 2,
) { connection ->
   connection.execSQL(
      "ALTER TABLE `Car` " +
         "ADD COLUMN `imagePaths` TEXT NOT NULL DEFAULT '[]'"
   )
}
