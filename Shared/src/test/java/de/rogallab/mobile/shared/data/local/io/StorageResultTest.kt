package de.rogallab.mobile.shared.data.local.io

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageResultTest {

   @Test
   fun success_returnsSuccessfulResult() {
      val result = storageResult { "stored" }

      assertTrue(result.isSuccess)
      assertEquals("stored", result.getOrNull())
   }

   @Test
   fun exception_isWrappedAsFailure() {
      val exception = IllegalStateException("failed")

      val result = storageResult<String> {
         throw exception
      }

      assertTrue(result.isFailure)
      assertSame(exception, result.exceptionOrNull())
   }

   @Test(expected = CancellationException::class)
   fun cancellationException_isRethrown() {
      storageResult<Unit> {
         throw CancellationException("cancelled")
      }
   }
}
