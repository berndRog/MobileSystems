package de.rogallab.mobile.data.local.database

import de.rogallab.mobile.data.ICarDao
import de.rogallab.mobile.data.IPersonDao
import de.rogallab.mobile.data.ITDriveDao
import de.rogallab.mobile.data.local.Seed
import de.rogallab.mobile.data.local.dtos.TDriveDto
import de.rogallab.mobile.shared.domain.io.IImageFileStorage
import de.rogallab.mobile.shared.domain.utilities.Alog
import kotlinx.coroutines.runBlocking
import java.util.Locale

class SeedDatabase(
   private val _personDao: IPersonDao,
   private val _carDao: ICarDao,
   private val _tDriveDao: ITDriveDao,
   private val _imageFileStorage: IImageFileStorage,
   private val _seed: Seed
) {
   fun seed() {

      runBlocking {
         if (_personDao.count() == 0) {
            _seed.createPersonDtos().forEach { personDto ->
               Alog.i("<-SeedDatabase", "PersonDto: $personDto")
               _personDao.insert(personDto)
            }
         }
         if (_carDao.count() == 0) {
            _seed.createCarDtos().forEach { carDto ->
               Alog.i("<-SeedDatabase", "CarDto: $carDto")
               _carDao.insert(carDto)
            }
         }


         if (_tDriveDao.count() == 0) {
            _seed.createsTestDriveDtos().forEach { tDriveDto ->
               _tDriveDao.insert(tDriveDto)
            }
         }
      }
   }
   }

/*
 * Didaktik und Lernziele
 *
 * - Die Personen verwenden dieselben Namen, stabilen IDs und Bilder wie die
 *   vorherigen Beispiele. Dadurch bleibt der bekannte Datenbestand erhalten.
 *
 * - Personenbilder sind Drawables der App. Beim Seeding werden sie mit
 *   IImageFileStorage in den privaten App-Speicher kopiert und Room speichert
 *   anschließend den absoluten Dateipfad.
 *
 * - Fahrzeuge besitzen mehrere Bildreferenzen. Die Seed-Fotos stammen aus
 *   Wikimedia Commons; Front-, Heck- und Innenraumansicht zeigen die Arbeit
 *   mit List<String> und dem zugehörigen Room-Column-Converter.
 *
 * - Zwei Fahrzeuge gehören Arne und zwei Berta. Die 1:n-Beziehung zwischen
 *   Person und Car lässt sich damit direkt in den Beispieldaten erkennen.
 *
 * - Die drei Probefahrten bilden beide Richtungen einer m:n-Beziehung ab:
 *   Frieda fährt mehrere Fahrzeuge und der Golf wird von mehreren Personen
 *   probegefahren.
 */
