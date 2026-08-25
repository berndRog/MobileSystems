package de.rogallab.mobile.shared.ui.effects

import kotlinx.coroutines.flow.Flow

/**
 * Exposes a stream of one-time UI effects.
 */
interface IEffectSource<E> {

   // Read-only stream consumed by the UI layer.
   val effects: Flow<E>
}

/*
 * Didaktik und Lernziele
 *
 * - IEffectSource<E> beschreibt nur die nach außen sichtbare Schnittstelle.
 *   Die konkrete Implementierung mit Channel und Flow liegt im EffectDelegate.
 *
 * - Der generische Typparameter E hält die Effect-Verarbeitung typsicher, zum
 *   Beispiel für PersonEffect oder PeopleEffect.
 *
 * Lernziele:
 *
 * - Interface und Implementierung voneinander trennen.
 * - Generische Schnittstellen für unterschiedliche Feature-Typen verwenden.
 */
