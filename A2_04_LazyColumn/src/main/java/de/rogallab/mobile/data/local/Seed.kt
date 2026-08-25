package de.rogallab.mobile.data.local

import de.rogallab.mobile.domain.entities.Person
import org.koin.core.component.KoinComponent
import java.util.Locale

class Seed() : KoinComponent {

   var people: MutableList<Person> = mutableListOf<Person>()

   fun createPeopleList() {
      val firstNames = mutableListOf(
         "Arne", "Berta", "Cord", "Dagmar", "Ernst", "Frieda", "Günter", "Hanna",
         "Ingo", "Johanna", "Klaus", "Luise", "Martin", "Nadja", "Otto", "Patrizia",
         "Quirin", "Rebecca", "Stefan", "Tanja", "Uwe", "Veronika", "Walter", "Xenia",
         "Yannick", "Zwantje")
      val lastNames = mutableListOf(
         "Arndt", "Bauer", "Conrad", "Diehl", "Engel", "Fischer", "Graf", "Hoffmann",
         "Imhoff", "Jung", "Klein", "Lang", "Meier", "Neumann", "Olbrich", "Peters",
         "Quart", "Richter", "Schmidt", "Thormann", "Ulrich", "Vogel", "Wagner", "Xander",
         "Yakov", "Zander")

      for (index in firstNames.indices) {
         val firstName = firstNames[index]
         val lastName = lastNames[index]
         val uuid = String.format(Locale.ROOT,
            "%02d000000-0000-0000-0000-000000000000", index + 1)
         val person = Person(firstName = firstName, lastName = lastName, id = uuid)
         people.add(person)
      }
   }
}