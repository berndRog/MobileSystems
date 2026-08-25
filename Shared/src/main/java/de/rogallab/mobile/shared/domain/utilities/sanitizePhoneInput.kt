package de.rogallab.mobile.shared.domain.utilities

// Filters phone input while the user types.
// Keeps digits and selected formatting characters.
// A plus sign is accepted only as the first character.
fun sanitizePhoneInput(
   input: String,
): String =
   buildString {
      input.forEach { character ->
         when {
            // Accept digits.
            character.isDigit() -> append(character)

            // Accept '+' only as the first character.
            character == '+' && isEmpty() -> append(character)

            // Accept common phone number formatting characters.
            character == ' ' -> append(character)
            character == '-' -> append(character)
            character == '/' -> append(character)
            character == '.' -> append(character)
            character == '(' -> append(character)
            character == ')' -> append(character)
         }
      }
   }
      // Limit the input to a reasonable maximum length.
      .take(30)

/*
 * Didaktik / Lernziele:
 *
 * - Eingaben können bereits während der Eingabe gefiltert werden.
 * - buildString ermöglicht den schrittweisen Aufbau eines neuen Strings.
 * - Mit when können unterschiedliche Regeln für einzelne Zeichen
 *   übersichtlich formuliert werden.
 * - Kontextabhängige Regeln sind möglich:
 *   '+' ist beispielsweise nur am Anfang zulässig.
 * - Formatierungszeichen wie Leerzeichen, '-', '/', '.', '(' und ')'
 *   bleiben erhalten, ohne ein bestimmtes Telefonnummernformat zu erzwingen.
 * - take(30) begrenzt die maximale Länge der Eingabe.
 *
 * Die Funktion prüft nicht, ob eine gültige Telefonnummer vorliegt.
 * Diese Aufgabe bleibt der anschließenden Validierung überlassen.
 */