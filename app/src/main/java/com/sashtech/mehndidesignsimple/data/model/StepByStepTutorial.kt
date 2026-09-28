package com.sashtech.mehndidesignsimple.data.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class StepByStepTutorial(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val categoryId: String = "step_by_step",
    val coverImageUrl: String = "",
    val difficulty: String = "Easy",
    val estimatedTime: String = "25 mins",
    val stepCount: Int = 0,
    val featured: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val type: String = "step_by_step"
) {
    val category: String get() = categoryId
}

@IgnoreExtraProperties
data class TutorialStep(
    val id: String = "",
    val stepNumber: Int = 1,
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val thumbnailUrl: String = ""
) {
    fun getDisplayImageUrl(): String {
        if (imageUrl.isNotBlank()) return imageUrl
        return thumbnailUrl
    }
}
