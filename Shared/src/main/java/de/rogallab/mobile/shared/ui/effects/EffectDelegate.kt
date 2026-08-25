package de.rogallab.mobile.shared.ui.effects

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Provides a reusable implementation for emitting one-time UI effects.
 * The generic type [E] represents the concrete effect type of a ViewModel.
 */
class EffectDelegate<E> : IEffectSource<E> {

   // Buffered channel for one-time effects such as messages or navigation.
   private val _effects = Channel<E>(Channel.BUFFERED)

   // Exposes the channel as a read-only Flow to the UI layer.
   override val effects: Flow<E> =
      _effects.receiveAsFlow()

   // Sends a new one-time effect to the effect stream.
   suspend fun emit(effect: E) {
      _effects.send(effect)
   }
}

/*
 * Didaktik und Lernziele
 *
 * - Der EffectDelegate kapselt die technische Verwaltung einmaliger UI-Effects.
 *
 * - Intern wird ein gepufferter Channel verwendet. receiveAsFlow() stellt
 *   diesen Channel nach außen als Flow bereit, ohne den Channel selbst
 *   zugänglich zu machen.
 *
 * - Das ViewModel muss Channel und Flow dadurch nicht selbst implementieren.
 *   Es kann die Implementierung des Interfaces delegieren:
 *
 *      class PersonViewModel(
 *         private val effectDelegate: EffectDelegate<PersonEffect>
 *      ) : ViewModel(),
 *         IEffectSource<PersonEffect> by effectDelegate
 *
 * - Das Schlüsselwort "by" übernimmt die Implementierung von effects aus dem
 *   Delegate. Das ViewModel bleibt auf seine fachlichen Aufgaben konzentriert.
 *
 * - Für einen Feature-Effect sollte genau eine UI-Stelle den Flow konsumieren.
 *   Mehrere parallele Collector desselben Channel-Flow würden einzelne Effects
 *   untereinander aufteilen und sind deshalb hier nicht vorgesehen.
 *
 * Lernziele:
 *
 * - Channel und Flow für einmalige UI-Effects einsetzen.
 * - Implementierungsdelegation mit "by" verstehen.
 * - Wiederverwendbare technische Logik aus ViewModels auslagern.
 * - State und einmalige Effects voneinander unterscheiden.
 */
