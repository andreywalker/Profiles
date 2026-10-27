package com.example.profiles
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.profiles.notifications.NotificationHelper
import com.example.profiles.ui.navigation.NavGraph
import com.example.profiles.ui.theme.ProfilesTheme
import com.example.profiles.ui.theme.ThemeMode
import com.example.profiles.utils.LocaleHelper

class MainActivity : ComponentActivity() {

    // Applies the saved language before anything is shown
    override fun attachBaseContext(newBase: Context) {
        val repo = (newBase.applicationContext as ProfilesApp).repository
        val lang = repo.getSettingBlocking("language") ?: "system"
        super.attachBaseContext(LocaleHelper.wrap(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = (application as ProfilesApp).repository
        val initialTheme = repo.getSettingBlocking("theme") ?: "DARK"
        val openPersonId = if (savedInstanceState == null)
            intent.getLongExtra(NotificationHelper.EXTRA_PERSON_ID, -1L).takeIf { it != -1L }
        else null
        setContent {
            val themeName by repo.observeSetting("theme").collectAsStateWithLifecycle(initialTheme)
            val mode = ThemeMode.valueOf(themeName ?: "DARK")
            val dark = when (mode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            // status/navigation bar icons follow the app theme, not only the system one
            DisposableEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }

            ProfilesTheme(mode = mode) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    NavGraph()
                }
            }
        }
    }
}