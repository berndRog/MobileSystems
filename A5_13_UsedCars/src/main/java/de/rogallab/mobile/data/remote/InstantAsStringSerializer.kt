package de.rogallab.mobile.data.remote

import kotlin.time.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object InstantAsStringSerializer : KSerializer<Instant> {
   override val descriptor: SerialDescriptor =
      PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

   override fun serialize(encoder: Encoder, value: Instant) {
      encoder.encodeString(value.toString())
   }

   override fun deserialize(decoder: Decoder): Instant =
      Instant.parse(decoder.decodeString())
}

/*
 * Didaktik und Lernziele
 *
 * - UsedCarsApi überträgt Zeitpunkte als UTC-Text im ISO-8601-Format.
 * - Der Serializer hält diese Transportregel in der Data-Schicht und ersetzt
 *   den früheren Gson-TypeAdapter beim Wechsel zu kotlinx.serialization.
 * - Die Domain verwendet weiterhin kotlin.time.Instant und kennt keine
 *   Retrofit- oder JSON-Klassen.
 */
