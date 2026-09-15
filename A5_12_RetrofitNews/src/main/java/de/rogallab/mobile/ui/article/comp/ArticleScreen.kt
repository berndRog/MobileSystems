package de.rogallab.mobile.ui.article.comp

import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.Article
import de.rogallab.mobile.ui.article.ArticleIntent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleScreen(
   article: Article,
   allowSave: Boolean,
   contentPadding: PaddingValues,
   onBack: () -> Unit,
   onIntent: (ArticleIntent) -> Unit,
) {
   Scaffold(
      modifier = Modifier.padding(contentPadding),
      contentWindowInsets = WindowInsets(0),
      topBar = {
         TopAppBar(
            windowInsets = WindowInsets(0),
            title = { Text(stringResource(R.string.article_title)) },
            navigationIcon = {
               IconButton(onClick = onBack) {
                  Icon(
                     imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                     contentDescription = stringResource(R.string.action_back),
                  )
               }
            },
         )
      },
      floatingActionButton = {
         if (allowSave) {
            FloatingActionButton(
               onClick = { onIntent(ArticleIntent.Save) }
            ) {
               Icon(
                  imageVector = Icons.Default.BookmarkAdd,
                  contentDescription = stringResource(R.string.action_save_article),
               )
            }
         }
      },
   ) { innerPadding ->
      AndroidView(
         modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
         factory = { context ->
            WebView(context).apply {
               layoutParams = ViewGroup.LayoutParams(
                  ViewGroup.LayoutParams.MATCH_PARENT,
                  ViewGroup.LayoutParams.MATCH_PARENT,
               )
               webViewClient = WebViewClient()
               settings.loadWithOverviewMode = true
               settings.javaScriptEnabled = true
               settings.domStorageEnabled = true
               loadUrl(article.url)
            }
         },
         update = { webView ->
            if (webView.url != article.url) {
               webView.loadUrl(article.url)
            }
         },
      )
   }
}
