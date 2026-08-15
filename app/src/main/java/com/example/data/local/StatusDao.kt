package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StatusDao {
    @Query("SELECT * FROM statuses ORDER BY dateModified DESC")
    fun getAllStatuses(): Flow<List<StatusEntity>>

    @Query("SELECT * FROM statuses WHERE isSaved = 1 ORDER BY COALESCE(savedDate, dateModified) DESC")
    fun getSavedStatuses(): Flow<List<StatusEntity>>

    @Query("SELECT * FROM statuses WHERE isFavorite = 1 ORDER BY dateModified DESC")
    fun getFavoriteStatuses(): Flow<List<StatusEntity>>

    @Query("SELECT * FROM statuses WHERE id = :id LIMIT 1")
    suspend fun getStatusById(id: String): StatusEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(status: StatusEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(statuses: List<StatusEntity>)

    @Update
    suspend fun update(status: StatusEntity)

    @Query("UPDATE statuses SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)

    @Query("UPDATE statuses SET isSaved = 1, savedFilePath = :savedPath, savedDate = :savedDate WHERE id = :id")
    suspend fun markAsSaved(id: String, savedPath: String, savedDate: Long)

    @Query("UPDATE statuses SET isNew = 0 WHERE id = :id")
    suspend fun markAsViewed(id: String)

    @Query("UPDATE statuses SET isNew = 0")
    suspend fun markAllAsViewed()

    @Query("UPDATE statuses SET isSaved = 0, savedFilePath = NULL, savedDate = NULL WHERE id = :id")
    suspend fun unmarkSaved(id: String)

    @Query("DELETE FROM statuses WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM statuses WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM statuses WHERE isSaved = 0")
    suspend fun clearUnsavedCache()

    @Query("UPDATE statuses SET isSaved = 0, savedFilePath = NULL, savedDate = NULL")
    suspend fun unmarkAllSaved()
}
