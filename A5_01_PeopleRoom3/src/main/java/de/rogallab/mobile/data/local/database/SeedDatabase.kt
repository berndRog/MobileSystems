package de.rogallab.mobile.data.local.database

import de.rogallab.mobile.data.IPersonDao
import de.rogallab.mobile.data.local.dtos.PersonDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SeedDatabase(
   private val _personDao: IPersonDao,
   private val _applicationScope: CoroutineScope,
) {
   fun start() {
      _applicationScope.launch {
         if (_personDao.count() == 0) {
            _personDao.insert(
               listOf(
                  PersonDto(
                     id = "4ad4894d-014e-4566-942d-41315b91a12c",
                     firstName = "Ada",
                     lastName = "Lovelace",
                     email = "ada.lovelace@example.org",
                     phone = "+44 20 7946 0101",
                     imagePath = null,
                  ),
                  PersonDto(
                     id = "f3952d14-88cf-4fa2-9ca7-71c63c739d97",
                     firstName = "Grace",
                     lastName = "Hopper",
                     email = "grace.hopper@example.org",
                     phone = "+1 703 555 0102",
                     imagePath = null,
                  ),
                  PersonDto(
                     id = "805ee8ec-8984-44da-b6e3-9fba3e4c3654",
                     firstName = "Alan",
                     lastName = "Turing",
                     email = "alan.turing@example.org",
                     phone = null,
                     imagePath = null,
                  ),
               )
            )
         }
      }
   }
}
