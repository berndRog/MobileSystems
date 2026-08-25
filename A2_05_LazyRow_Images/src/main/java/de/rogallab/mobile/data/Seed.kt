package de.rogallab.mobile.data

import android.content.Context
import androidx.core.content.ContextCompat.getString
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.DogImage

class Seed(
   _context: Context
) {
   private var _dogs: MutableList<DogImage> = mutableListOf()
   val dogs: List<DogImage> = _dogs as List<DogImage>

   init {
      _dogs.add(
         DogImage(
            name = getString(_context, R.string.dog_01),
            resourcePicture = R.drawable.dog_01
         )
      )
      _dogs.add(DogImage(getString(_context, R.string.dog_02), R.drawable.dog_02))
      _dogs.add(DogImage(getString(_context, R.string.dog_03), R.drawable.dog_03))
      _dogs.add(DogImage(getString(_context, R.string.dog_04), R.drawable.dog_04))
      _dogs.add(DogImage(getString(_context, R.string.dog_05), R.drawable.dog_05))
      _dogs.add(DogImage(getString(_context, R.string.dog_06), R.drawable.dog_06))
      _dogs.add(DogImage(getString(_context, R.string.dog_07), R.drawable.dog_07))
      _dogs.add(DogImage(getString(_context, R.string.dog_08), R.drawable.dog_08))
      // sort dogs by name
      _dogs = _dogs.sortedBy { dog -> dog.name } as MutableList<DogImage>      // Initialize seed data if necessary
   }
}