package de.rogallab.mobile.shared.ui.lists

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * Makes a target item visible in a LazyColumn or LazyRow after a state change.
 *
 * The item is only scrolled into view when it is currently outside the visible
 * viewport. The target key is acknowledged after the check so the same trigger
 * is not processed repeatedly.
 */
@Composable
fun <T, K> ScrollToItemIfNotVisible(
   listState: LazyListState,
   targetKey: K?,
   items: List<T>,
   keyOf: (T) -> K,
   onHandled: () -> Unit,
) {
   LaunchedEffect(targetKey) {
      val key = targetKey ?: return@LaunchedEffect

      val index = items.indexOfFirst { item ->
         keyOf(item) == key
      }

      if (index >= 0) {
         val isVisible = listState.layoutInfo.visibleItemsInfo.any { itemInfo ->
            itemInfo.index == index
         }

         if (!isVisible) {
            listState.animateScrollToItem(index)
         }
      }

      onHandled()
   }
}

/*
 * Didaktik und Lernziele
 *
 * - ScrollToItemIfNotVisible ist eine kleine, generische UI-Hilfsfunktion für
 *   LazyColumn und LazyRow. Sie kennt weder Person noch PeopleViewModel.
 *
 * - targetKey modelliert einen einmaligen Auftrag: Nach Undo soll der zuvor
 *   entfernte Eintrag wieder sichtbar sein. Die Funktion sucht dazu den Index
 *   des Elements in der aktuellen Liste.
 *
 * - Über LazyListState.layoutInfo.visibleItemsInfo wird zuerst geprüft, ob das
 *   Ziel bereits im sichtbaren Bereich liegt. Nur wenn es außerhalb liegt,
 *   wird mit animateScrollToItem(...) gescrollt.
 *
 * - onHandled() bestätigt die Verarbeitung des Auftrags. Das ViewModel kann
 *   anschließend den targetKey wieder auf null setzen und verhindert damit,
 *   dass derselbe Scroll-Auftrag mehrfach ausgeführt wird.
 *
 * Lernziele:
 *
 * - LazyListState zur gezielten Steuerung einer LazyColumn verwenden.
 * - Einmalige UI-Aufträge über State und anschließendes Acknowledge modellieren.
 * - Generische UI-Hilfsfunktionen unabhängig von konkreten Entities entwerfen.
 */
