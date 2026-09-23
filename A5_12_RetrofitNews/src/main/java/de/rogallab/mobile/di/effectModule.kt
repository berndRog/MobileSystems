package de.rogallab.mobile.di

import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.ui.article.ArticleEffect
import de.rogallab.mobile.ui.articles.ArticlesEffect
import de.rogallab.mobile.ui.news.NewsEffect
import org.koin.core.qualifier.named
import org.koin.dsl.module

val newsEffectQualifier = named("newsEffect")
val articlesEffectQualifier = named("articlesEffect")
val articleEffectQualifier = named("articleEffect")

fun effectModule() = module {
   // Every ViewModel receives its own buffered effect channel.
   factory<EffectDelegate<NewsEffect>>(newsEffectQualifier) { EffectDelegate() }
   factory<EffectDelegate<ArticlesEffect>>(articlesEffectQualifier) { EffectDelegate() }
   factory<EffectDelegate<ArticleEffect>>(articleEffectQualifier) { EffectDelegate() }
}

/*
 * Didaktik und Lernziele
 *
 * - Qualifier unterscheiden die drei EffectDelegate-Typen innerhalb von Koin.
 *   factory erzeugt für jedes ViewModel eine eigene Effect-Pipeline.
 *
 * Lernziele:
 *
 * - Generische Abhängigkeiten mit Qualifiern eindeutig registrieren.
 * - Kurzlebige ViewModel-nahe Infrastruktur als Factory bereitstellen.
 */
