package io.github.vulka

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import io.github.vulka.ui.VulkaNavigation
import io.github.vulka.ui.theme.VulkaTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            Log.e("Vulka", "Uncaught exception", e)
            finish()
        }

        enableEdgeToEdge()

        setContent {
            VulkaTheme {
                VulkaNavigation()
            }
        }
    }
}
