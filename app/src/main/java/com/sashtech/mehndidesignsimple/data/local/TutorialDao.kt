package com.sashtech.mehndidesignsimple.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TutorialDao {

    @Query("SELECT * FROM cached_tutorials ORDER BY id ASC LIMIT :limit")
    fun getTutorials(limit: Int = 50): Flow<List<CachedTutorialEntity>>

    @Query("SELECT * FROM cached_tutorials WHERE id = :id LIMIT 1")
    fun getTutorialById(id: String): Flow<CachedTutorialEntity?>

    @Query("SELECT COUNT(*) FROM cached_tutorials")
    suspend fun getTutorialCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTutorials(tutorials: List<CachedTutorialEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTutorial(tutorial: CachedTutorialEntity)

    @Query("DELETE FROM cached_tutorials WHERE id NOT IN (:validIds)")
    suspend fun removeStaleTutorials(validIds: List<String>)

    @Query("DELETE FROM cached_tutorials WHERE id = :id")
    suspend fun deleteTutorial(id: String)

    @Query("DELETE FROM cached_tutorials")
    suspend fun clearAllTutorials()

    @Query("SELECT * FROM cached_tutorial_steps WHERE tutorialId = :tutorialId ORDER BY stepNumber ASC")
    fun getStepsForTutorial(tutorialId: String): Flow<List<CachedTutorialStepEntity>>

    @Query("DELETE FROM cached_tutorial_steps WHERE tutorialId = :tutorialId")
    suspend fun deleteTutorialSteps(tutorialId: String)

    @Query("SELECT COUNT(*) FROM cached_tutorial_steps WHERE tutorialId = :tutorialId")
    suspend fun getStepCountForTutorial(tutorialId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTutorialSteps(steps: List<CachedTutorialStepEntity>)
}
