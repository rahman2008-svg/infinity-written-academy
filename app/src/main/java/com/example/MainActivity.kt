package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.about.AboutDeveloperScreen
import com.example.ui.bookmarks.BookmarksScreen
import com.example.ui.home.HomeScreen
import com.example.ui.navigation.Screen
import com.example.ui.practice.PracticeScreen
import com.example.ui.reader.ReaderScreen
import com.example.ui.revision.QuickRevisionScreen
import com.example.ui.search.SearchScreen
import com.example.ui.section.CategoryListScreen
import com.example.ui.section.SectionHubScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.WrittenAcademyTheme
import com.example.ui.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val appViewModel: AppViewModel = viewModel()
            val themeMode by appViewModel.themeMode.collectAsState()
            val isDark = when (themeMode) {
                "Dark" -> true
                "Light" -> false
                else -> isSystemInDarkTheme()
            }

            WrittenAcademyTheme(darkTheme = isDark) {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route
                ) {
                    // Home Dashboard
                    composable(Screen.Home.route) {
                        HomeScreen(
                            viewModel = appViewModel,
                            onNavigateToBengaliHub = { navController.navigate(Screen.BengaliHub.route) },
                            onNavigateToEnglishHub = { navController.navigate(Screen.EnglishHub.route) },
                            onNavigateToQuickRevision = { navController.navigate(Screen.QuickRevision.route) },
                            onNavigateToPractice = { itemId ->
                                navController.navigate(Screen.Practice.createRoute(itemId))
                            },
                            onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                            onNavigateToBookmarks = { navController.navigate(Screen.Bookmarks.route) },
                            onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                            onOpenItem = { itemId ->
                                navController.navigate(Screen.Reader.createRoute(itemId))
                            },
                            onNavigateToAboutDeveloper = {
                                navController.navigate(Screen.AboutDeveloper.route)
                            }
                        )
                    }

                    // Bengali Hub
                    composable(Screen.BengaliHub.route) {
                        SectionHubScreen(
                            language = "Bengali",
                            viewModel = appViewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onCategoryClick = { category ->
                                navController.navigate(Screen.CategoryList.createRoute(category, "Bengali"))
                            }
                        )
                    }

                    // English Hub
                    composable(Screen.EnglishHub.route) {
                        SectionHubScreen(
                            language = "English",
                            viewModel = appViewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onCategoryClick = { category ->
                                navController.navigate(Screen.CategoryList.createRoute(category, "English"))
                            }
                        )
                    }

                    // Category Item List
                    composable(
                        route = Screen.CategoryList.route,
                        arguments = listOf(
                            navArgument("category") { type = NavType.StringType },
                            navArgument("language") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val category = backStackEntry.arguments?.getString("category") ?: ""
                        val language = backStackEntry.arguments?.getString("language") ?: "Bengali"
                        CategoryListScreen(
                            category = category,
                            language = language,
                            viewModel = appViewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onOpenItem = { itemId ->
                                navController.navigate(Screen.Reader.createRoute(itemId))
                            }
                        )
                    }

                    // Content Reader Screen
                    composable(
                        route = Screen.Reader.route,
                        arguments = listOf(navArgument("itemId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
                        ReaderScreen(
                            itemId = itemId,
                            viewModel = appViewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToPractice = { currentItemId ->
                                navController.navigate(Screen.Practice.createRoute(currentItemId))
                            }
                        )
                    }

                    // Practice Room Screen
                    composable(
                        route = Screen.Practice.route,
                        arguments = listOf(
                            navArgument("itemId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        val itemId = backStackEntry.arguments?.getString("itemId")
                        PracticeScreen(
                            initialItemId = itemId,
                            viewModel = appViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    // Quick Revision Screen
                    composable(Screen.QuickRevision.route) {
                        QuickRevisionScreen(
                            viewModel = appViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    // Offline Search Screen
                    composable(Screen.Search.route) {
                        SearchScreen(
                            viewModel = appViewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onOpenItem = { itemId ->
                                navController.navigate(Screen.Reader.createRoute(itemId))
                            }
                        )
                    }

                    // Bookmarks Screen
                    composable(Screen.Bookmarks.route) {
                        BookmarksScreen(
                            viewModel = appViewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onOpenItem = { itemId ->
                                navController.navigate(Screen.Reader.createRoute(itemId))
                            }
                        )
                    }

                    // Settings Screen
                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            viewModel = appViewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToAboutDeveloper = {
                                navController.navigate(Screen.AboutDeveloper.route)
                            }
                        )
                    }

                    // About Developer Screen
                    composable(Screen.AboutDeveloper.route) {
                        AboutDeveloperScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
