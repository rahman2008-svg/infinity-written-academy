package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object BengaliHub : Screen("bengali_hub")
    object EnglishHub : Screen("english_hub")
    object QuickRevision : Screen("quick_revision")
    object Search : Screen("search")
    object Bookmarks : Screen("bookmarks")
    object Settings : Screen("settings")
    object AboutDeveloper : Screen("about_developer")
    object Practice : Screen("practice?itemId={itemId}") {
        fun createRoute(itemId: String? = null) = if (itemId != null) "practice?itemId=$itemId" else "practice"
    }
    object CategoryList : Screen("category/{category}/{language}") {
        fun createRoute(category: String, language: String) = "category/$category/$language"
    }
    object Reader : Screen("reader/{itemId}") {
        fun createRoute(itemId: String) = "reader/$itemId"
    }
}
