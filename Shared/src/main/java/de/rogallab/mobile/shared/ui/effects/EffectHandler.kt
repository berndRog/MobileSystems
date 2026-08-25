package de.rogallab.mobile.shared.ui.effects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import de.rogallab.mobile.shared.domain.utilities.Alog
import kotlinx.coroutines.flow.Flow

/**
 * Collects one-time effects and forwards them to the given handler.
 */
@Composable
fun <E> EffectHandler(
   effects: Flow<E>,
   onEffect: suspend (E) -> Unit,
) {
   LaunchedEffect(effects) {
      effects.collect { effect ->
         Alog.i("<-EffectHandler", "effect: $effect")
         onEffect(effect)
      }
   }
}

/*
 * Didaktik und Lernziele
 *
 * - EffectHandler ist generisch und kennt keinen konkreten Effect-Typ.
 *
 * - Der Handler übernimmt ausschließlich das Sammeln des Effect-Flow.
 *   Die fachliche Auswertung bleibt beim jeweiligen Adapter.
 *
 * - Dadurch kann dieselbe Funktion für PersonEffect, PeopleEffect und
 *   weitere Effect-Typen wiederverwendet werden.
 *
 * Lernziele:
 *
 * - Generische Composables einsetzen.
 * - Flow-basierte Effects mit LaunchedEffect sammeln.
 * - Technisches Flow-Collecting und fachliche Effect-Auswertung trennen.
 */
