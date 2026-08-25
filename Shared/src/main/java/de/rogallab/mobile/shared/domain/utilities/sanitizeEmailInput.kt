package de.rogallab.mobile.shared.domain.utilities

import java.text.Normalizer

// Filters email input while the user types.
// The result contains only a restricted set of ASCII characters
// commonly used in email addresses.
fun sanitizeEmailInput(
   input: String
): String {

   val nfd = Normalizer.normalize(input, Normalizer.Form.NFD)
   val out = StringBuilder()
   var i = 0

   while (i < nfd.length) {
      val c = nfd[i]
      val next = nfd.getOrNull(i + 1)

      // Dead-key first: ¨a / ¨o / ¨u
      if ((c == '\u00A8' || c == '\u0308') && next != null) {
         when (next) {
            'a' -> { out.append("ae"); i += 2; continue }
            'o' -> { out.append("oe"); i += 2; continue }
            'u' -> { out.append("ue"); i += 2; continue }
            'A' -> { out.append("Ae"); i += 2; continue }
            'O' -> { out.append("Oe"); i += 2; continue }
            'U' -> { out.append("Ue"); i += 2; continue }
         }
      }

      // Base letter + combining diaeresis: ä / ö / ü
      if (next == '\u0308') {
         when (c) {
            'a' -> { out.append("ae"); i += 2; continue }
            'o' -> { out.append("oe"); i += 2; continue }
            'u' -> { out.append("ue"); i += 2; continue }
            'A' -> { out.append("Ae"); i += 2; continue }
            'O' -> { out.append("Oe"); i += 2; continue }
            'U' -> { out.append("Ue"); i += 2; continue }
         }
      }

      when (c) {
         'ä' -> out.append("ae")
         'ö' -> out.append("oe")
         'ü' -> out.append("ue")
         'Ä' -> out.append("Ae")
         'Ö' -> out.append("Oe")
         'Ü' -> out.append("Ue")
         'ß' -> out.append("ss")
         else -> out.append(c)
      }
      i++
   }

   val normalized = Normalizer.normalize(out.toString(), Normalizer.Form.NFD)
   return normalized.filter { ch ->
      ch in 'a'..'z' || ch in 'A'..'Z' || ch in '0'..'9' ||
         ch == '@' || ch == '.' || ch == '_' || ch == '-' || ch == '+'
   }
}

/*
 * Didaktik / Lernziele:
 *
 * - Eingaben können bereits während der Eingabe transformiert werden.
 * - Unicode-Zeichen können aus einem Grundzeichen und zusätzlichen
 *   diakritischen Zeichen bestehen.
 * - Normalizer.Form.NFD zerlegt z. B. é in e + Akzent.
 * - Sprachspezifische Regeln wie ä -> ae müssen vor der allgemeinen
 *   Unicode-Normalisierung behandelt werden.
 * - filter erlaubt eine explizite Whitelist zulässiger Zeichen.
 *
 * Die Funktion prüft nicht, ob eine syntaktisch gültige E-Mail-Adresse
 * vorliegt. Diese Aufgabe bleibt der anschließenden Validierung überlassen.
 */