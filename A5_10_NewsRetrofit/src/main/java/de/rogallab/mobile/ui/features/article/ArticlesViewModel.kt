package de.rogallab.mobile.ui.features.article

import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.data.dtos.Article
import de.rogallab.mobile.domain.IArticleRepository
import de.rogallab.mobile.domain.ResultData
import de.rogallab.mobile.ui.base.BaseViewModel
import de.rogallab.mobile.ui.navigation.ArticleWeb
import de.rogallab.mobile.ui.navigation.INavHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// A R T I C L E S   L I S T   S C R E E N
class ArticlesViewModel(
   private val _repository: IArticleRepository,
   private val _navHandler: INavHandler,
) : BaseViewModel(_navHandler, TAG) {

   val articlesUiStateFlow: StateFlow<ArticlesUiState> =
      _repository.selectArticles()
         .map { result: Result<List<Article>> ->
            result.fold(
               onSuccess = { articles ->
                  ArticlesUiState(loading = false, articles = articles)
               },
               onFailure = { t ->
                  handleErrorEvent(t)
                  ArticlesUiState(loading = false, articles = emptyList())
               }
            )
         }
         .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ArticlesUiState(loading = true)
         )

   // W E B   A R T I C L E   S C R E E N
   private var _webArticleUiStateFlow = MutableStateFlow(WebArticleUiState())
   val webArticleUiStateFlow: StateFlow<WebArticleUiState> = _webArticleUiStateFlow.asStateFlow()

   // transform intent into an action
   fun onProcessIntent(intent: ArticleIntent) {
      when (intent) {
         is ArticleIntent.ShowWebArticle -> selectArticle(intent.isNews, intent.article)
         is ArticleIntent.SaveArticle -> upsert()
         is ArticleIntent.RemoveArticle -> remove(intent.article)
         is ArticleIntent.UndoRemoveArticle -> undoRemove()
      }
   }

   private fun selectArticle(isNews: Boolean, article: Article) {
      _webArticleUiStateFlow.update { current: WebArticleUiState ->
         current.copy(isNews = isNews, article = article)
      }
      _navHandler.push(ArticleWeb)
      //onNavigate(NavEvent.NavigateForward(NavScreen.WebArticleScreen.route))
   }

   private fun upsert() {
      _webArticleUiStateFlow.value.article?.let { article ->
         viewModelScope.launch() {
            _repository.upsert(article).fold(
               onSuccess = { },
               onFailure = { t: Throwable -> handleErrorEvent(t) }
            )
         }
      }
   }

   private var _removedArticle: Article? = null

   private fun remove(article: Article) {
      viewModelScope.launch {
         _repository.remove(article).fold(
            onSuccess = { _removedArticle = article },
            onFailure = { t: Throwable -> handleErrorEvent(t) }
         )
      }
   }

   private fun undoRemove() {
      _removedArticle?.let { article ->
         viewModelScope.launch {
            _repository.upsert(article).fold(
               onSuccess = { _removedArticle = null },
               onFailure = { t: Throwable -> handleErrorEvent(t) }
            )
         }
      }
   }

   companion object {
      private const val TAG = "<-ArticlesViewModel"
   }
}