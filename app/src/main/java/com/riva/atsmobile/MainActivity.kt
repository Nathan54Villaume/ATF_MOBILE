package com.riva.atsmobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsCompat
import com.riva.atsmobile.ui.ATSMobileApp
import com.riva.atsmobile.ui.theme.ATSMobileTheme
import com.riva.atsmobile.viewmodel.SelectionViewModel
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.riva.atsmobile.utils.NetworkMonitor
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: SelectionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Enregistre l’observateur réseau réactif
        NetworkMonitor.register(applicationContext)

        // ✅ Applique le comportement de base pour les barres système
        WindowCompat.setDecorFitsSystemWindows(window, true)

        // 🔄 Réagit dynamiquement au rôle après connexion
        lifecycleScope.launch {
            viewModel.role.collectLatest { role ->
                if (role.equals("ADMIN", ignoreCase = true)) {
                    // Affiche la barre système
                    WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.statusBars())
                } else {
                    // Masque la barre système
                    WindowInsetsControllerCompat(window, window.decorView).apply {
                        hide(WindowInsetsCompat.Type.statusBars())
                        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                }
            }
        }

        // ✅ Interface principale
        setContent {
            ATSMobileTheme {
                ATSMobileApp(viewModel = viewModel)
            }
        }
    }
}
