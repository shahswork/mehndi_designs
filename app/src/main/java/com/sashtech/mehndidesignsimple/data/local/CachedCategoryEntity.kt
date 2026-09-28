package com.sashtech.mehndidesignsimple.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sashtech.mehndidesignsimple.data.model.MehndiCategory

@Entity(tableName = "cached_categories")
data class CachedCategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val imageUrl: String,
    val isStepByStep: Boolean,
    val defaultDesignCount: Int,
    val sortOrder: Int = 0,
    val isActive: Boolean = true
) {
    fun toMehndiCategory(): MehndiCategory = MehndiCategory(
        id = id,
        name = name,
        description = description,
        imageUrl = imageUrl,
        isStepByStep = isStepByStep,
        defaultDesignCount = defaultDesignCount,
        isActive = isActive
    )

    companion object {
        fun fromMehndiCategory(category: MehndiCategory, order: Int = 0): CachedCategoryEntity =
            CachedCategoryEntity(
                id = category.id,
                name = category.name,
                description = category.description,
                imageUrl = category.imageUrl,
                isStepByStep = category.isStepByStep,
                defaultDesignCount = category.defaultDesignCount,
                sortOrder = order,
                isActive = category.isActive
            )
    }
}
