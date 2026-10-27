package com.example.profiles.ui.navigation


import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.profiles.ui.edit.EditScreen
import com.example.profiles.ui.main.MainScreen

object Routes {
    const val MAIN = "main"
    const val PROFILE = "profile/{id}"
    const val EDIT = "edit?id={id}"
    const val SETTINGS = "settings"
    const val NOTIFICATIONS = "notifications"

    fun profile(id: Long) = "profile/$id"
    fun edit(id: Long? = null) = if (id == null) "edit" else "edit?id=$id"
}

@Composable
fun NavGraph() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.MAIN) {
        composable(Routes.MAIN) {
        MainScreen(
            onPersonClick = { id -> nav.navigate(Routes.edit(id)) },
            onAddClick = { nav.navigate(Routes.edit()) },
            onSettingsClick = { nav.navigate(Routes.SETTINGS) },
            onNotificationsClick = { nav.navigate(Routes.NOTIFICATIONS) }
        )
    }
        composable(
            Routes.PROFILE,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            Text("Profile ${entry.arguments?.getLong("id")}")
        }
        composable(
            Routes.EDIT,
            arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L })
        ) {
            EditScreen(onDone = { nav.popBackStack() })
        }
        composable(Routes.SETTINGS) { Text("Settings") }
        composable(Routes.NOTIFICATIONS) { Text("Notifications") }
    }
}