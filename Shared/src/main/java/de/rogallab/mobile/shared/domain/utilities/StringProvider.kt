package de.rogallab.mobile.shared.domain.utilities

import android.content.Context
import androidx.annotation.StringRes
import de.rogallab.mobile.shared.domain.IStringProvider

class StringProvider(
   private val context: Context
) : IStringProvider {

   override fun getString(
      @StringRes resId: Int,
      vararg args: Any
   ): String = context.getString(resId, *args)
}