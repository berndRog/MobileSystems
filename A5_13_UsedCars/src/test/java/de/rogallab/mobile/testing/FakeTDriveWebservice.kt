package de.rogallab.mobile.testing

import de.rogallab.mobile.data.remote.ITDriveWebservice
import de.rogallab.mobile.data.remote.dtos.TDriveDto
import retrofit2.Response

class FakeTDriveWebservice : ITDriveWebservice {
   var drives: List<TDriveDto> = emptyList()
   var deleteResponse: Response<Unit> = Response.success(Unit)
   var deletedId: String? = null

   override suspend fun getAll() = drives
   override suspend fun countAll() = drives.size
   override suspend fun getById(id: String) = drives.first { it.id == id }
   override suspend fun create(tDrive: TDriveDto): TDriveDto = error("unused")
   override suspend fun update(id: String, tDrive: TDriveDto): TDriveDto = error("unused")
   override suspend fun delete(id: String): Response<Unit> {
      deletedId = id
      return deleteResponse
   }
}
