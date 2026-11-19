package de.rogallab.mobile.data.local

import android.app.Application
import androidx.room3.Room
import androidx.room3.useReaderConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import de.rogallab.mobile.data.local.database.AppDatabase
import de.rogallab.mobile.data.local.dtos.ArticleDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ArticleDaoTest {
   private lateinit var database: AppDatabase
   private lateinit var dao: IArticleDao
   private val article = ArticleDto(
      "https://example.test/article", "News", null, "Original", null, null,
      "2026-01-01T12:00:00Z", null,
   )

   @Before
   fun setUp() {
      database = Room.inMemoryDatabaseBuilder(
         ApplicationProvider.getApplicationContext<android.content.Context>(),
         AppDatabase::class.java,
      ).setDriver(AndroidSQLiteDriver()).build()
      dao = database.createArticleDao()
   }

   @After
   fun tearDown() {
      database.close()
   }

   @Test
   fun save_returnsSQLiteRowIdAndPreservesUrl() = runTest {
      val rowId: Long = dao.save(article)

      assertTrue(rowId > 0L)
      database.useReaderConnection { connection ->
         connection.usePrepared("SELECT rowid FROM Article WHERE url = ?") { statement ->
            statement.bindText(1, article.url)
            assertTrue(statement.step())
            assertEquals(rowId, statement.getLong(0))
         }
      }
      assertEquals(listOf(article), dao.observeAll().first())
   }

   @Test
   fun save_existingUrl_replacesArticleAndReturnsValidRowId() = runTest {
      dao.save(article)
      val changed = article.copy(title = "Changed")

      assertTrue(dao.save(changed) > 0L)
      assertEquals(listOf(changed), dao.observeAll().first())
   }

   @Test
   fun remove_existingUrl_returnsOneAndRemovesArticle() = runTest {
      dao.save(article)

      assertEquals(1, dao.remove(article.url))
      assertTrue(dao.observeAll().first().isEmpty())
   }

   @Test
   fun remove_missingUrl_returnsZero() = runTest {
      assertEquals(0, dao.remove(article.url))
   }
}
