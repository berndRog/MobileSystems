package de.rogallab.mobile.shared.domain

import androidx.annotation.StringRes

interface IStringProvider {
   fun getString(
      @StringRes resId: Int,
      vararg args: Any
   ): String
}