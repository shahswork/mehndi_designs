package com.sashtech.mehndidesignsimple.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY savedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteDesignEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE id = :designId)")
    fun isFavorite(designId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE id = :designId)")
    suspend fun isFavoriteSync(designId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteDesignEntity)

    @Query("DELETE FROM favorites WHERE id = :designId")
    suspend fun deleteFavorite(designId: String)
}
