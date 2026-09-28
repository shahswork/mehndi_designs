package com.sashtech.mehndidesignsimple.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sashtech.mehndidesignsimple.data.model.MehndiDesign

@Entity(tableName = "favorites")
data class FavoriteDesignEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val imageUrl: String = "",
    val thumbnailUrl: String = "",
    val mediumImageUrl: String = "",
    val difficulty: String = "Easy",
    val estimatedTime: String = "20 mins",
    val isTutorial: Boolean = false,
    val savedAt: Long = System.currentTimeMillis()
) {
    fun toMehndiDesign(): MehndiDesign {
        return MehndiDesign(
            id = id,
            title = title,
            description = description,
            categoryId = categoryId,
            categoryName = categoryName,
            imageUrl = imageUrl,
            thumbnailUrl = thumbnailUrl,
            mediumImageUrl = mediumImageUrl,
            difficulty = difficulty,
            estimatedTime = estimatedTime,
            type = if (isTutorial) "step_by_step" else "design"
        )
    }

    companion object {
        fun fromMehndiDesign(design: MehndiDesign): FavoriteDesignEntity {
            return FavoriteDesignEntity(
                id = design.id,
                title = design.title,
                description = design.description,
                categoryId = design.categoryId.ifEmpty { design.category },
                categoryName = design.categoryName,
                imageUrl = design.imageUrl ?: "",
                thumbnailUrl = design.thumbnailUrl ?: "",
                mediumImageUrl = design.mediumImageUrl ?: "",
                difficulty = design.difficulty,
                estimatedTime = design.estimatedTime,
                isTutorial = design.type == "step_by_step"
            )
        }
    }
}
