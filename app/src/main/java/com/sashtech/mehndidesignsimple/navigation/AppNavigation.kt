package com.sashtech.mehndidesignsimple.navigation

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.sashtech.mehndidesignsimple.ads.AdManager
import com.sashtech.mehndidesignsimple.ads.AdMobBanner
import com.sashtech.mehndidesignsimple.data.repository.MehndiRepository
import com.sashtech.mehndidesignsimple.ui.screens.CategoriesScreen
import com.sashtech.mehndidesignsimple.ui.screens.CategoryDetailScreen
import com.sashtech.mehndidesignsimple.ui.screens.DesignDetailScreen
import com.sashtech.mehndidesignsimple.ui.screens.FavoritesScreen
import com.sashtech.mehndidesignsimple.ui.screens.HomeScreen
import com.sashtech.mehndidesignsimple.ui.screens.SearchScreen
import com.sashtech.mehndidesignsimple.ui.screens.SettingsScreen
import com.sashtech.mehndidesignsimple.ui.screens.SplashScreen
import com.sashtech.mehndidesignsimple.ui.screens.StepByStepViewerScreen
import com.sashtech.mehndidesignsimple.viewmodel.CategoriesViewModel
import com.sashtech.mehndidesignsimple.viewmodel.CategoryDesignsViewModel
import com.sashtech.mehndidesignsimple.viewmodel.DesignDetailViewModel
import com.sashtech.mehndidesignsimple.viewmodel.FavoritesViewModel
import com.sashtech.mehndidesignsimple.viewmodel.HomeViewModel
import com.sashtech.mehndidesignsimple.viewmodel.SearchViewModel
import com.sashtech.mehndidesignsimple.viewmodel.SplashViewModel
import com.sashtech.mehndidesignsimple.viewmodel.StepByStepViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    repository: MehndiRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val adManager = remember { AdManager.getInstance(context) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val openDesignDetail: (String) -> Unit = { designId ->
        adManager.onDesignAction(activity) {
            navController.navigate(Screen.DesignDetail.createRoute(designId))
        }
    }

    val openTutorial: (String) -> Unit = { tutorialId ->
        adManager.onDesignAction(activity) {
            navController.navigate(Screen.StepByStepViewer.createRoute(tutorialId))
        }
    }

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Categories.route,
        Screen.Favorites.route,
        Screen.Settings.route
    )

    val navigateToTab: (String) -> Unit = { route ->
        if (route == Screen.Home.route) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Home.route) {
                    inclusive = false
                    saveState = false
                }
                launchSingleTop = true
            }
        } else {
            navController.navigate(route) {
                popUpTo(Screen.Home.route) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (currentRoute != Screen.Splash.route) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AdMobBanner(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (!showBottomBar) Modifier.navigationBarsPadding() else Modifier
                            ),
                        adManager = adManager
                    )
                    if (showBottomBar) {
                        MehndiBottomBar(
                            currentRoute = currentRoute,
                            onNavigate = navigateToTab
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Splash Screen
            composable(Screen.Splash.route) {
                val splashViewModel: SplashViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return SplashViewModel(repository) as T
                        }
                    }
                )
                SplashScreen(
                    viewModel = splashViewModel,
                    onNavigateToHome = { isOffline ->
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }

            // Home Screen
            composable(Screen.Home.route) {
                val homeViewModel: HomeViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return HomeViewModel(repository) as T
                        }
                    }
                )
                HomeScreen(
                    viewModel = homeViewModel,
                    onDesignClick = { design ->
                        openDesignDetail(design.id)
                    },
                    onTutorialClick = { tutorial ->
                        openTutorial(tutorial.id)
                    },
                    onSearchClick = {
                        navController.navigate(Screen.Search.route)
                    },
                    onViewAllCategoriesClick = {
                        navigateToTab(Screen.Categories.route)
                    },
                    onCategoryClick = { categoryId ->
                        navController.navigate(Screen.CategoryDetail.createRoute(categoryId))
                    }
                )
            }

            // Categories Screen
            composable(Screen.Categories.route) {
                val categoriesViewModel: CategoriesViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return CategoriesViewModel(repository) as T
                        }
                    }
                )
                CategoriesScreen(
                    viewModel = categoriesViewModel,
                    onCategoryClick = { category ->
                        navController.navigate(Screen.CategoryDetail.createRoute(category.id))
                    },
                    onTutorialClick = { tutorial ->
                        openTutorial(tutorial.id)
                    },
                    onSearchClick = {
                        navController.navigate(Screen.Search.route)
                    }
                )
            }

            // Category Detail Screen
            composable(
                route = Screen.CategoryDetail.route,
                arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
            ) { backStackEntry ->
                val categoryId = backStackEntry.arguments?.getString("categoryId") ?: "front_hand"
                val categoryViewModel: CategoryDesignsViewModel = viewModel(
                    key = "cat_$categoryId",
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return CategoryDesignsViewModel(categoryId, repository) as T
                        }
                    }
                )
                CategoryDetailScreen(
                    viewModel = categoryViewModel,
                    onDesignClick = { design ->
                        if (design.type == "step_by_step") {
                            openTutorial(design.id)
                        } else {
                            openDesignDetail(design.id)
                        }
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }

            // Design Detail Screen
            composable(
                route = Screen.DesignDetail.route,
                arguments = listOf(navArgument("designId") { type = NavType.StringType })
            ) { backStackEntry ->
                val designId = backStackEntry.arguments?.getString("designId") ?: ""
                val detailViewModel: DesignDetailViewModel = viewModel(
                    key = "detail_$designId",
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return DesignDetailViewModel(designId, repository) as T
                        }
                    }
                )
                DesignDetailScreen(
                    viewModel = detailViewModel,
                    onDesignClick = { design ->
                        openDesignDetail(design.id)
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }

            // Step By Step Tutorial Viewer Screen
            composable(
                route = Screen.StepByStepViewer.route,
                arguments = listOf(navArgument("tutorialId") { type = NavType.StringType })
            ) { backStackEntry ->
                val tutorialId = backStackEntry.arguments?.getString("tutorialId") ?: ""
                val stepViewModel: StepByStepViewModel = viewModel(
                    key = "step_$tutorialId",
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return StepByStepViewModel(tutorialId, repository) as T
                        }
                    }
                )
                StepByStepViewerScreen(
                    viewModel = stepViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            // Favorites Screen
            composable(Screen.Favorites.route) {
                val favViewModel: FavoritesViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return FavoritesViewModel(repository) as T
                        }
                    }
                )
                FavoritesScreen(
                    viewModel = favViewModel,
                    onDesignClick = { design ->
                        if (design.type == "step_by_step") {
                            openTutorial(design.id)
                        } else {
                            openDesignDetail(design.id)
                        }
                    },
                    onSearchClick = {
                        navController.navigate(Screen.Search.route)
                    },
                    onExploreClick = {
                        navigateToTab(Screen.Home.route)
                    }
                )
            }

            // Search Screen
            composable(Screen.Search.route) {
                val searchViewModel: SearchViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return SearchViewModel(repository) as T
                        }
                    }
                )
                SearchScreen(
                    viewModel = searchViewModel,
                    onDesignClick = { design ->
                        openDesignDetail(design.id)
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }

            // Settings Screen
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
