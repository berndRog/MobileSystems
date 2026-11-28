package de.rogallab.mobile.shared.ui.effects

import androidx.annotation.StringRes
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.domain.utilities.Alog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * Resolves message texts and sends typed effects through the existing delegate.
 * Only launch-prefixed methods start a new coroutine in the supplied scope.
 */
class EffectDispatcher<E>(
   private val _scope: CoroutineScope,
   private val _effectDelegate: EffectDelegate<E>,
   private val _stringProvider: IStringProvider,
   private val _errorEffect: (String) -> E,
   private val _tag: String,
   private val _messageEffect: ((String) -> E)? = null,
) {
   suspend fun emit(effect: E) {
      // Keep the caller's context, including its cancellation and lifetime.
      currentCoroutineContext().ensureActive()
      _effectDelegate.emit(effect)
   }

   fun launchEmit(effect: E): Job =
      _scope.launch { emit(effect) }

   suspend fun showError(
      message: String,
      throwable: Throwable? = null,
   ) {
      // Cancellation is control flow, never a logged or displayed failure.
      if (throwable is CancellationException) throw throwable
      currentCoroutineContext().ensureActive()

      // Validation messages without a cause do not need an additional error log.
      if (throwable != null) Alog.e(_tag, message, throwable)
      emit(_errorEffect(message))
   }

   suspend fun showError(
      @StringRes resId: Int,
      throwable: Throwable? = null,
      vararg args: Any,
   ) {
      // Check before resolving resources, not only before sending the effect.
      if (throwable is CancellationException) throw throwable
      currentCoroutineContext().ensureActive()
      showError(_stringProvider.getString(resId, *args), throwable)
   }

   fun launchShowError(
      @StringRes resId: Int,
      throwable: Throwable? = null,
      vararg args: Any,
   ): Job = _scope.launch { showError(resId, throwable, *args) }

   fun launchShowError(
      message: String,
      throwable: Throwable? = null,
   ): Job = _scope.launch { showError(message, throwable) }

   suspend fun showMessage(message: String) {
      currentCoroutineContext().ensureActive()
      // Error-only features need no artificial ShowMessage effect.
      val create = checkNotNull(_messageEffect) {
         "No message effect factory configured"
      }
      emit(create(message))
   }

   suspend fun showMessage(
      @StringRes resId: Int,
      vararg args: Any,
   ) {
      currentCoroutineContext().ensureActive()
      showMessage(_stringProvider.getString(resId, *args))
   }

   fun launchShowMessage(
      @StringRes resId: Int,
      vararg args: Any,
   ): Job = _scope.launch { showMessage(resId, *args) }

   fun launchShowMessage(message: String): Job =
      _scope.launch { showMessage(message) }
}

/*
 * Didaktik und Lernziele
 *
 * - EffectDispatcher bereitet Meldungen auf; EffectDelegate transportiert sie.
 *   Er verwendet denselben Delegate wie IEffectSource im ViewModel und besitzt
 *   weder einen eigenen Channel noch einen eigenen CoroutineScope.
 *
 * - Factory-Funktionen erzeugen die typisierten Effects des jeweiligen Features.
 *   Eine optionale Message-Factory unterstützt Features mit reinen Fehlermeldungen.
 *
 * - showError, showMessage und emit sind suspendierend und bleiben im Kontext
 *   des Aufrufers. Nur launchShowError, launchShowMessage und launchEmit starten
 *   eine neue Coroutine im übergebenen Scope und liefern deren Job zurück.
 *
 * - Zusammengehörige Effects werden innerhalb derselben Coroutine nacheinander
 *   gesendet. Ein abgeschlossener Versand bestätigt noch keine UI-Verarbeitung.
 *
 * - Ressourcen und Formatargumente werden über IStringProvider aufgelöst.
 *   Bereits aufgelöste Texte bleiben unverändert. Nur Fehler mit Throwable
 *   werden zentral protokolliert; derselbe Fehler wird am Aufrufer nicht erneut
 *   geloggt. Cancellation wird vor Ressourcenauflösung und Logging weitergegeben.
 *
 * - Persistente Ladefehler, Validierungsentscheidungen und fachliche Abläufe
 *   bleiben im ViewModel. Der Dispatcher übernimmt keine Zustandsverwaltung.
 *
 * Lernziele:
 *
 * - Aufbereitung, Transport und UI-Verarbeitung von Effects unterscheiden.
 * - Generische Infrastruktur mit typisierten Factory-Funktionen konfigurieren.
 * - Suspendierende Aufrufe von neu gestarteten Coroutines unterscheiden.
 * - Cancellation, Effect-Reihenfolge und Scope-Lebensdauer berücksichtigen.
 */
