package com.sashtech.mehndidesignsimple.data.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class MehndiCategory(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val isStepByStep: Boolean = false,
    val defaultDesignCount: Int = 0,
    val isActive: Boolean = true
) {
    companion object {
        fun formatCategoryName(id: String): String {
            if (id.isBlank()) return "Mehndi"
            return id.replace("_", " ")
                .split(" ")
                .joinToString(" ") { word ->
                    word.replaceFirstChar { char ->
                        if (char.isLowerCase()) char.titlecase() else char.toString()
                    }
                }
        }
    }
}
