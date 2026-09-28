package com.sashtech.mehndidesignsimple.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sashtech.mehndidesignsimple.data.model.MehndiDesign

@Entity(tableName = "cached_designs")
data class CachedDesignEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val imageUrl: String = "",
    val thumbnailUrl: String = "",
    val mediumImageUrl: String = "",
    val featured: Boolean = false,
    val popular: Boolean = false,
    val difficulty: String = "Easy",
    val estimatedTime: String = "20 mins",
    val tagsCsv: String = "",
    val searchKeywordsCsv: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val type: String = "design",
    val cachedAt: Long = System.currentTimeMillis()
) {
    fun toMehndiDesign(): MehndiDesign {
        val tagsList = if (tagsCsv.isBlank()) emptyList() else tagsCsv.split(",").map { it.trim() }
        val keywordsList = if (searchKeywordsCsv.isBlank()) emptyList() else searchKeywordsCsv.split(",").map { it.trim() }

        return MehndiDesign(
            id = id,
            title = title,
            description = description,
            categoryId = categoryId,
            categoryName = categoryName,
            imageUrl = imageUrl,
            thumbnailUrl = thumbnailUrl,
            mediumImageUrl = mediumImageUrl,
            featured = featured,
            popular = popular,
            difficulty = difficulty,
            estimatedTime = estimatedTime,
            tags = tagsList,
            searchKeywords = keywordsList,
            createdAt = createdAt,
            type = type
        )
    }

    companion object {
        fun fromMehndiDesign(design: MehndiDesign): CachedDesignEntity {
            return CachedDesignEntity(
                id = design.id,
                title = design.title,
                description = design.description,
                categoryId = design.categoryId.ifEmpty { design.category },
                categoryName = design.categoryName,
                imageUrl = design.imageUrl ?: "",
                thumbnailUrl = design.thumbnailUrl ?: "",
                mediumImageUrl = design.mediumImageUrl ?: "",
                featured = design.featured,
                popular = design.popular,
                difficulty = design.difficulty,
                estimatedTime = design.estimatedTime,
                tagsCsv = design.tags.joinToString(","),
                searchKeywordsCsv = design.searchKeywords.joinToString(","),
                createdAt = design.createdAt,
                type = design.type,
                cachedAt = System.currentTimeMillis()
            )
        }
    }
}
