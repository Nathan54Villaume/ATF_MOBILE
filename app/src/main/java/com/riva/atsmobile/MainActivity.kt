package com.riva.atsmobile

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.riva.atsmobile.ui.ATSMobileApp
import com.riva.atsmobile.ui.theme.ATSMobileTheme
import com.riva.atsmobile.utils.NetworkMonitor
import com.riva.atsmobile.viewmodel.SelectionViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: SelectionViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // Démarrage du monitoring réseau
        NetworkMonitor.register(applicationContext)
        //viewModel.InitNetworkObserverIfNeeded(this)

        // Chargement session utilisateur et gammes
        viewModel.chargerSessionLocale(this)
        viewModel.chargerGammesDepuisApi(this)

        // Masquage ou affichage dynamique de la barre de statut
        WindowCompat.setDecorFitsSystemWindows(window, true)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.role.collectLatest { role ->
                    val controller = WindowInsetsControllerCompat(window, window.decorView)
                    if (role.equals("ADMIN", ignoreCase = true)) {
                        controller.show(WindowInsetsCompat.Type.statusBars())
                    } else {
                        controller.hide(WindowInsetsCompat.Type.statusBars())
                        controller.systemBarsBehavior =
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                }
            }
        }

        // Affichage de l'application principale avec thème
        setContent {
            ATSMobileTheme {
                ATSMobileApp(viewModel = viewModel)
            }
        }
    }
}
