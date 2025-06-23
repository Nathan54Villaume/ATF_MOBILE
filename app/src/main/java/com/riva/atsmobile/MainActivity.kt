package com.riva.atsmobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.riva.atsmobile.ui.ATSMobileApp
import com.riva.atsmobile.ui.theme.ATSMobileTheme
import com.riva.atsmobile.utils.NetworkMonitor
import com.riva.atsmobile.viewmodel.SelectionViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        private const val PERM_REQUEST_LOCATION = 1001
    }

    // Hilt injecte automatiquement le VM annoté @HiltViewModel
    private val viewModel: SelectionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) Permission localisation
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERM_REQUEST_LOCATION
            )
        }

        // 2) NetworkMonitor global
        NetworkMonitor.register(applicationContext)

        // 3) Démarrage de l’observer réseau dans le VM
        viewModel.InitNetworkObserverIfNeeded(this)

        // 4) Charger la session et les gammes
        viewModel.chargerSessionLocale(this)
        viewModel.chargerGammesDepuisApi(this)

        // 5) Config barres système selon rôle
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

        // 6) Compose
        setContent {
            ATSMobileTheme {
                // Ne passe plus viewModel, ATSMobileApp l’obtient via hiltViewModel()
                ATSMobileApp()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERM_REQUEST_LOCATION &&
            grantResults.firstOrNull() != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(
                this,
                "La permission de localisation est requise pour certaines fonctionnalités réseau.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
