package com.sashtech.mehndidesignsimple.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Categories : Screen("categories")
    object Favorites : Screen("favorites")
    object Settings : Screen("settings")
    object Search : Screen("search")

    object CategoryDetail : Screen("category/{categoryId}") {
        fun createRoute(categoryId: String) = "category/$categoryId"
    }

    object DesignDetail : Screen("design/{designId}") {
        fun createRoute(designId: String) = "design/$designId"
    }

    object StepByStep : Screen("step-by-step")

    object StepByStepViewer : Screen("step-by-step/{tutorialId}") {
        fun createRoute(tutorialId: String) = "step-by-step/$tutorialId"
    }
}

data class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

val BOTTOM_NAV_ITEMS = listOf(
    BottomNavItem(
        route = Screen.Home.route,
        title = "Home",
        selectedIcon = Icons.Outlined.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_home"
    ),
    BottomNavItem(
        route = Screen.Categories.route,
        title = "Categories",
        selectedIcon = Icons.Outlined.GridView,
        unselectedIcon = Icons.Outlined.GridView,
        testTag = "nav_categories"
    ),
    BottomNavItem(
        route = Screen.Favorites.route,
        title = "Favorites",
        selectedIcon = Icons.Outlined.FavoriteBorder,
        unselectedIcon = Icons.Outlined.FavoriteBorder,
        testTag = "nav_favorites"
    ),
    BottomNavItem(
        route = Screen.Settings.route,
        title = "Settings",
        selectedIcon = Icons.Outlined.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_settings"
    )
)
