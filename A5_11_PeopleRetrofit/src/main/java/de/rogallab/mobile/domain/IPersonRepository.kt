package de.rogallab.mobile.domain

import de.rogallab.mobile.domain.entities.Person
import kotlinx.coroutines.flow.Flow

interface IPersonRepository {
   fun observeAll(): Flow<Result<List<Person>>>
   suspend fun findById(id: String): Result<Person?>

   suspend fun create(person: Person): Result<Unit>
   suspend fun update(person: Person): Result<Unit>
   suspend fun remove(person: Person): Result<Unit>
}

/*
 * Didaktik und Lernziele
 *
 * - Der Repository-Port ist gegenüber A5_01 absichtlich unverändert.
 * - ViewModels erkennen deshalb nicht, ob Personendaten aus Room oder über HTTP
 *   geladen werden. Nur die Data-Schicht tauscht ihre Implementierung aus.
 * - Diese stabile Schnittstelle macht den Architekturvorteil des Repository-
 *   Patterns beim Wechsel der Datenquelle direkt sichtbar.
 */
