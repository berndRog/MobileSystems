package de.rogallab.mobile.shared.data.local

fun checkInserted(rowId: Long) {
   check(rowId != -1L) {
      "Insert failed: No row inserted"
   }
}

fun checkUpdated(rowsAffected: Int) {
   check(rowsAffected == 1) {
      "Update failed: Expected 1 row, got $rowsAffected"
   }
}

fun checkDeleted(rowsAffected: Int) {
   check(rowsAffected == 1) {
      "Delete failed: Expected 1 row, got $rowsAffected"
   }
}