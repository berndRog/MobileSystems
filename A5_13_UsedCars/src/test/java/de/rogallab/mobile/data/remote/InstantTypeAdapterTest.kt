package de.rogallab.mobile.data.remote

import com.google.gson.GsonBuilder
import de.rogallab.mobile.data.remote.dtos.TDriveDto
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class InstantTypeAdapterTest {
   private val gson = GsonBuilder()
      .registerTypeAdapter(Instant::class.java, InstantTypeAdapter())
      .create()

   @Test
   fun instant_isSerializedAsUtcIso8601AndParsedAgain() {
      val dto = TDriveDto(
         id = "t1",
         personId = "p1",
         carId = "c1",
         start = Instant.parse("2026-10-15T08:30:00Z"),
         isCompleted = false,
      )

      val json = gson.toJson(dto)
      val restored = gson.fromJson(json, TDriveDto::class.java)

      assertEquals(true, json.contains("\"start\":\"2026-10-15T08:30:00Z\""))
      assertEquals(dto, restored)
   }
}
