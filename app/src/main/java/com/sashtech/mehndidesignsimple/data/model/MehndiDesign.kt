package com.sashtech.mehndidesignsimple.data.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class MehndiDesign(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val imageUrl: String? = "",
    val thumbnailUrl: String? = "",
    val mediumImageUrl: String? = "",
    val featured: Boolean = false,
    val popular: Boolean = false,
    val difficulty: String = "Easy",
    val estimatedTime: String = "20 mins",
    val tags: List<String> = emptyList(),
    val searchKeywords: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val type: String = "design"
) {
    // Backward compatibility helper
    val category: String
        get() = categoryId.ifEmpty { "simple" }

    /**
     * Returns canonical difficulty string ("Easy", "Medium", "Advanced")
     */
    fun getNormalizedDifficulty(): String = normalizeDifficulty(difficulty)

    companion object {
        /**
         * Normalizes raw difficulty strings from Firebase or user input.
         * Treats "intermediate" (Firebase representation) as "Medium".
         */
        fun normalizeDifficulty(raw: String?): String {
            val trimmed = raw?.trim() ?: return "Easy"
            return when {
                trimmed.equals("intermediate", ignoreCase = true) ||
                trimmed.equals("medium", ignoreCase = true) ||
                trimmed.equals("med", ignoreCase = true) ||
                trimmed.equals("inter", ignoreCase = true) -> "Medium"

                trimmed.equals("easy", ignoreCase = true) ||
                trimmed.equals("simple", ignoreCase = true) ||
                trimmed.equals("beginner", ignoreCase = true) ||
                trimmed.equals("basic", ignoreCase = true) -> "Easy"

                trimmed.equals("advanced", ignoreCase = true) ||
                trimmed.equals("hard", ignoreCase = true) ||
                trimmed.equals("expert", ignoreCase = true) ||
                trimmed.equals("pro", ignoreCase = true) ||
                trimmed.equals("complex", ignoreCase = true) -> "Advanced"

                trimmed.isNotBlank() -> trimmed.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase() else it.toString()
                }
                else -> "Easy"
            }
        }

        /**
         * Checks if a design's difficulty matches the selected filter.
         * Handles mapping between "Medium" and "intermediate" seamlessly.
         */
        fun matchesDifficulty(designDifficulty: String?, filterDifficulty: String?): Boolean {
            val filter = filterDifficulty?.trim() ?: "All"
            if (filter.equals("All", ignoreCase = true)) return true

            val normalizedFilter = normalizeDifficulty(filter)
            val normalizedDesign = normalizeDifficulty(designDifficulty)

            return normalizedFilter.equals(normalizedDesign, ignoreCase = true)
        }
    }

    /**
     * Resolves the primary grid thumbnail URL dynamically from Firebase.
     * Hierarchy: thumbnailUrl -> mediumImageUrl -> imageUrl.
     * Returns empty string if no valid image URL exists so that placeholder is shown.
     */
    fun getDisplayThumbnailUrl(): String {
        val thumb = thumbnailUrl?.trim() ?: ""
        if (thumb.isNotBlank()) return thumb

        val medium = mediumImageUrl?.trim() ?: ""
        if (medium.isNotBlank()) return medium

        val full = imageUrl?.trim() ?: ""
        if (full.isNotBlank()) return full

        return ""
    }

    /**
     * Resolves the medium-resolution image URL for detail views dynamically.
     * Hierarchy: mediumImageUrl -> imageUrl -> thumbnailUrl.
     */
    fun getDisplayMediumUrl(): String {
        val medium = mediumImageUrl?.trim() ?: ""
        if (medium.isNotBlank()) return medium

        val full = imageUrl?.trim() ?: ""
        if (full.isNotBlank()) return full

        val thumb = thumbnailUrl?.trim() ?: ""
        if (thumb.isNotBlank()) return thumb

        return ""
    }

    /**
     * Resolves the high-resolution full image URL for zooming & downloading dynamically.
     * Hierarchy: imageUrl -> mediumImageUrl -> thumbnailUrl.
     */
    fun getDisplayFullUrl(): String {
        val full = imageUrl?.trim() ?: ""
        if (full.isNotBlank()) return full

        val medium = mediumImageUrl?.trim() ?: ""
        if (medium.isNotBlank()) return medium

        val thumb = thumbnailUrl?.trim() ?: ""
        if (thumb.isNotBlank()) return thumb

        return ""
    }

    // Convenience aliases
    fun getGridImageUrl(): String = getDisplayThumbnailUrl()
    fun getDetailImageUrl(): String = getDisplayMediumUrl()
    fun getDownloadImageUrl(): String = getDisplayFullUrl()
}
