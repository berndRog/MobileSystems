package de.rogallab.mobile.ui.features.news

import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import de.rogallab.mobile.data.dtos.News
import de.rogallab.mobile.domain.INewsRepository
import de.rogallab.mobile.domain.utilities.logDebug
import de.rogallab.mobile.ui.base.BaseViewModel
import de.rogallab.mobile.ui.navigation.INavHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class NewsViewModel(
   private val _repository: INewsRepository,
   private val _imageLoader: ImageLoader,
   navHandler: INavHandler,
) : BaseViewModel(navHandler, TAG) {

   // N E W S   L I S T   S C R E E N
   private var _newsUiStateFlow: MutableStateFlow<NewsUiState> = MutableStateFlow(NewsUiState())

   // Refreshable Scenario, fetch news from webApi
   private val reloadTrigger = MutableSharedFlow<Unit>(replay = 1)
   var everythingPage = 1
   val newsUiStateFlow: StateFlow<NewsUiState> = reloadTrigger
      .flatMapLatest {
         // set loading true before starting the request
         _newsUiStateFlow.update { current ->
            current.copy(loading = true)
         }

         _repository.getEverything(
            _searchUiStateFlow.value.searchText,
            everythingPage
         ).map { result: Result<News> ->
            result.fold(
               onSuccess = { news ->
                  _newsUiStateFlow.update { current -> current.copy(loading = false, news = news) }
                  _newsUiStateFlow.value
               },
               onFailure = { throwable ->
                  handleErrorEvent(throwable)
                  _newsUiStateFlow.update { current -> current.copy(loading = false) }
                  _newsUiStateFlow.value
               }
            )
         }
      }.stateIn(
         scope = viewModelScope,
         started = SharingStarted.WhileSubscribed(),
         initialValue = NewsUiState(loading = true)
      )

   fun triggerSearch() {
      viewModelScope.launch {
         reloadTrigger.emit(Unit)
      }
   }

   // S E A R C H   B A R   O N   T O P
   private var _searchUiStateFlow: MutableStateFlow<SearchUiState> = MutableStateFlow(SearchUiState())
   val searchUiStateFlow: StateFlow<SearchUiState> = _searchUiStateFlow.asStateFlow()

   // transform intent into an action
   fun onProcessIntent(intent: NewsIntent) {
      logDebug(TAG, "onProcessIntent: $intent")
      when (intent) {
         is NewsIntent.SearchTextChange -> onSearchChange(intent.searchText)
         is NewsIntent.TriggerSearch -> triggerSearch()
      }
   }

   private fun onSearchChange(searchText: String) {
      logDebug(TAG, "searchText: ($searchText) (${_searchUiStateFlow.value.searchText})")
      if (searchText == _searchUiStateFlow.value.searchText) return
      _searchUiStateFlow.update { it ->
         it.copy(searchText = searchText)
      }
   }

   @OptIn(ExperimentalCoilApi::class)
   override fun onCleared() {
      logDebug(TAG, "onCleared(): clear caches")
      _imageLoader.memoryCache?.clear()
      _imageLoader.diskCache?.clear()
      super.onCleared()
   }


   companion object {
      private const val TAG = "<-NewsViewModel"
   }
}
