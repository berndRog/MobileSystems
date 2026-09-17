package de.rogallab.mobile.data.remote

import de.rogallab.mobile.data.remote.dtos.TDriveDto
import kotlin.time.Instant
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class InstantAsStringSerializerTest {
   private val json = Json

   @Test
   fun instant_isSerializedAsUtcIso8601AndParsedAgain() {
      val dto = TDriveDto(
         id = "t1",
         personId = "p1",
         carId = "c1",
         start = Instant.parse("2026-10-15T08:30:00Z"),
         isCompleted = false,
      )

      val jsonText = json.encodeToString(TDriveDto.serializer(), dto)
      val restored = json.decodeFromString(TDriveDto.serializer(), jsonText)

      assertEquals(true, jsonText.contains("\"start\":\"2026-10-15T08:30:00Z\""))
      assertEquals(dto, restored)
   }
}
