package de.rogallab.mobile.domain

import de.rogallab.mobile.domain.entities.DogImage

interface IImageRepository {
   fun getAll(): Result<List<DogImage>>
}