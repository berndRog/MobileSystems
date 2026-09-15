package de.rogallab.mobile.testing

import de.rogallab.mobile.shared.domain.IStringProvider

class FakeStringProvider : IStringProvider {
   override fun getString(resId: Int, vararg args: Any): String =
      "res-$resId" +
         if (args.isEmpty()) ""
         else ":${args.joinToString()}"
}
