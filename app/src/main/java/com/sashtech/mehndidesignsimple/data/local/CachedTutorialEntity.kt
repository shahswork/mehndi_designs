package com.sashtech.mehndidesignsimple.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sashtech.mehndidesignsimple.data.model.StepByStepTutorial
import com.sashtech.mehndidesignsimple.data.model.TutorialStep

@Entity(tableName = "cached_tutorials")
data class CachedTutorialEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String = "",
    val coverImageUrl: String = "",
    val categoryId: String = "step_by_step",
    val difficulty: String = "Easy",
    val estimatedTime: String = "20 mins",
    val stepCount: Int = 0,
    val featured: Boolean = false,
    val type: String = "step_by_step",
    val cachedAt: Long = System.currentTimeMillis()
) {
    fun toStepByStepTutorial(): StepByStepTutorial {
        return StepByStepTutorial(
            id = id,
            title = title,
            description = description,
            coverImageUrl = coverImageUrl,
            categoryId = categoryId,
            difficulty = difficulty,
            estimatedTime = estimatedTime,
            stepCount = stepCount,
            featured = featured,
            type = type
        )
    }

    companion object {
        fun fromStepByStepTutorial(tutorial: StepByStepTutorial): CachedTutorialEntity {
            return CachedTutorialEntity(
                id = tutorial.id,
                title = tutorial.title,
                description = tutorial.description,
                coverImageUrl = tutorial.coverImageUrl,
                categoryId = tutorial.categoryId,
                difficulty = tutorial.difficulty,
                estimatedTime = tutorial.estimatedTime,
                stepCount = tutorial.stepCount,
                featured = tutorial.featured,
                type = tutorial.type,
                cachedAt = System.currentTimeMillis()
            )
        }
    }
}

@Entity(tableName = "cached_tutorial_steps")
data class CachedTutorialStepEntity(
    @PrimaryKey
    val id: String,
    val tutorialId: String,
    val stepNumber: Int,
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val thumbnailUrl: String = ""
) {
    fun toTutorialStep(): TutorialStep {
        return TutorialStep(
            id = id,
            stepNumber = stepNumber,
            title = title,
            description = description,
            imageUrl = imageUrl,
            thumbnailUrl = thumbnailUrl
        )
    }

    companion object {
        fun fromTutorialStep(tutorialId: String, step: TutorialStep): CachedTutorialStepEntity {
            val stepId = step.id.ifEmpty { "${tutorialId}_step_${step.stepNumber}" }
            return CachedTutorialStepEntity(
                id = stepId,
                tutorialId = tutorialId,
                stepNumber = step.stepNumber,
                title = step.title,
                description = step.description,
                imageUrl = step.imageUrl,
                thumbnailUrl = step.thumbnailUrl
            )
        }
    }
}
