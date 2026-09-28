package com.sashtech.mehndidesignsimple.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM cached_categories WHERE isActive = 1 ORDER BY sortOrder ASC, name ASC")
    fun getAllCategories(): Flow<List<CachedCategoryEntity>>

    @Query("SELECT * FROM cached_categories WHERE id = :categoryId LIMIT 1")
    fun getCategoryById(categoryId: String): Flow<CachedCategoryEntity?>

    @Query("SELECT * FROM cached_categories WHERE id = :categoryId LIMIT 1")
    suspend fun getCategoryByIdSync(categoryId: String): CachedCategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CachedCategoryEntity>)

    @Query("DELETE FROM cached_categories")
    suspend fun clearAllCategories()

    @Query("DELETE FROM cached_categories WHERE id NOT IN (:validIds)")
    suspend fun removeStaleCategories(validIds: List<String>)

    @Query("SELECT COUNT(*) FROM cached_categories")
    suspend fun getCategoryCount(): Int
}
