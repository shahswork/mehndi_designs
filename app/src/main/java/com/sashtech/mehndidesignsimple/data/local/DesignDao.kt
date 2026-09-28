package com.sashtech.mehndidesignsimple.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DesignDao {

    @Query("SELECT * FROM cached_designs ORDER BY createdAt DESC")
    fun getAllDesigns(): Flow<List<CachedDesignEntity>>

    @Query("SELECT * FROM cached_designs WHERE featured = 1 ORDER BY createdAt DESC LIMIT :limit")
    fun getFeaturedDesigns(limit: Int): Flow<List<CachedDesignEntity>>

    @Query("SELECT * FROM cached_designs WHERE popular = 1 ORDER BY createdAt DESC LIMIT :limit")
    fun getPopularDesigns(limit: Int): Flow<List<CachedDesignEntity>>

    @Query("SELECT * FROM cached_designs ORDER BY createdAt DESC LIMIT :limit")
    fun getLatestDesigns(limit: Int): Flow<List<CachedDesignEntity>>

    @Query("SELECT * FROM cached_designs WHERE LOWER(categoryId) = LOWER(:categoryId) ORDER BY createdAt DESC LIMIT :limit")
    fun getDesignsByCategory(categoryId: String, limit: Int): Flow<List<CachedDesignEntity>>

    @Query("SELECT * FROM cached_designs WHERE id = :id LIMIT 1")
    fun getDesignById(id: String): Flow<CachedDesignEntity?>

    @Query("SELECT * FROM cached_designs WHERE id = :id LIMIT 1")
    suspend fun getDesignByIdSync(id: String): CachedDesignEntity?

    @Query("SELECT COUNT(*) FROM cached_designs")
    suspend fun getDesignCount(): Int

    @Query("SELECT * FROM cached_designs")
    suspend fun getAllDesignsSync(): List<CachedDesignEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDesigns(designs: List<CachedDesignEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDesign(design: CachedDesignEntity)

    @Query("DELETE FROM cached_designs WHERE id NOT IN (:validIds)")
    suspend fun removeStaleDesigns(validIds: List<String>)

    @Query("DELETE FROM cached_designs WHERE id = :id")
    suspend fun deleteDesign(id: String)

    @Query("DELETE FROM cached_designs")
    suspend fun clearAll()

    @Query("""
        SELECT * FROM cached_designs 
        WHERE LOWER(title) LIKE '%' || LOWER(:query) || '%' 
           OR LOWER(categoryId) LIKE '%' || LOWER(:query) || '%' 
           OR LOWER(categoryName) LIKE '%' || LOWER(:query) || '%' 
           OR LOWER(description) LIKE '%' || LOWER(:query) || '%' 
           OR LOWER(tagsCsv) LIKE '%' || LOWER(:query) || '%' 
           OR LOWER(searchKeywordsCsv) LIKE '%' || LOWER(:query) || '%'
        ORDER BY createdAt DESC
    """)
    fun searchDesigns(query: String): Flow<List<CachedDesignEntity>>
}
