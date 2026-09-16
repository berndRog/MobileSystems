package de.rogallab.mobile.data.mapping

import de.rogallab.mobile.data.remote.dtos.CarDto
import de.rogallab.mobile.data.remote.dtos.PersonDto
import de.rogallab.mobile.data.remote.dtos.TDriveDto
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteMappingsTest {
   @Test
   fun personMapping_mapsImageUrlToDomainImagePath() {
      val dto = PersonDto("p1", "Ada", "Lovelace", null, null, "https://host/ada.jpg")

      val person = dto.toPerson()

      assertEquals(dto.imageUrl, person.imagePath)
      assertEquals(dto, person.toPersonDto())
   }

   @Test
   fun carMapping_mapsAllImageUrlsAndSeller() {
      val dto = CarDto("c1", "Fiat", "500", 18_900, listOf("https://host/1.jpg"), "p1")

      val car = dto.toCar()

      assertEquals(dto.imageUrls, car.imagePaths)
      assertEquals(dto.personId, car.personId)
      assertEquals(dto, car.toCarDto())
   }

   @Test
   fun testDriveMapping_preservesUtcInstant() {
      val start = Instant.parse("2026-10-15T08:30:00Z")
      val dto = TDriveDto("t1", "p1", "c1", start, false)

      val testDrive = dto.toTestDrive()

      assertEquals(start, testDrive.start)
      assertEquals(dto, testDrive.toTestDriveDto())
   }
}
