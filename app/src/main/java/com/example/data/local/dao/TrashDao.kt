package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.TrashItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrashDao {
    @Query("SELECT * FROM trash_items ORDER BY deletedTimestamp DESC")
    fun getAllTrash(): Flow<List<TrashItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrash(item: TrashItemEntity): Long

    @Query("SELECT * FROM trash_items WHERE id = :id LIMIT 1")
    suspend fun getTrashById(id: Int): TrashItemEntity?

    @Query("DELETE FROM trash_items WHERE id = :id")
    suspend fun deleteTrashById(id: Int)

    @Query("DELETE FROM trash_items")
    suspend fun clearAll()
}
