package de.rogallab.mobile.data.repositories

import de.rogallab.mobile.data.IPersonDao
import de.rogallab.mobile.data.local.dtos.PersonDto
import de.rogallab.mobile.data.local.relations.PersonWithCarsDto
import de.rogallab.mobile.data.local.relations.PersonWithTestDriveCarsDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakePersonDao : IPersonDao {
   private val _people = MutableStateFlow<List<PersonDto>>(emptyList())

   override fun observeAll(): Flow<List<PersonDto>> = _people
   override suspend fun findById(personId: String): PersonDto? =
      _people.value.firstOrNull { it.id == personId }
   override suspend fun findWithCars(personId: String): PersonWithCarsDto? =
      findById(personId)?.let { PersonWithCarsDto(it, emptyList()) }
   override suspend fun findWithTestDriveCars(personId: String): PersonWithTestDriveCarsDto? =
      findById(personId)?.let { PersonWithTestDriveCarsDto(it, emptyList()) }
   override suspend fun count(): Int = _people.value.size
   override suspend fun insert(personDto: PersonDto) {
      check(_people.value.none { it.id == personDto.id })
      _people.value = _people.value + personDto
   }
   override suspend fun insert(personDtos: List<PersonDto>) { personDtos.forEach { insert(it) } }
   override suspend fun update(personDto: PersonDto): Int {
      if (_people.value.none { it.id == personDto.id }) return 0
      _people.value = _people.value.map { if (it.id == personDto.id) personDto else it }
      return 1
   }
   override suspend fun delete(personDto: PersonDto): Int {
      val oldSize = _people.value.size
      _people.value = _people.value.filterNot { it.id == personDto.id }
      return if (_people.value.size < oldSize) 1 else 0
   }
}
