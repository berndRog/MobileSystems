package de.rogallab.mobile.testing

import de.rogallab.mobile.shared.domain.IStringProvider

class FakeStringProvider : IStringProvider {
   override fun getString(resId: Int, vararg args: Any): String =
      "res-$resId" +
         if (args.isEmpty()) ""
         else ":${args.joinToString()}"
}

/*
 * Didaktik und Lernziele
 *
 * - Der Fake liefert stabile, prüfbare Texte ohne Android-Ressourcensystem.
 *   Tests vergleichen damit die ausgewählte Resource-ID statt eine Übersetzung.
 *
 * Lernziele:
 *
 * - Plattformabhängige Abhängigkeiten durch kleine Test-Doubles ersetzen.
 */
