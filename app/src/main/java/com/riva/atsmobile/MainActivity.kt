package com.riva.atsmobile

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.riva.atsmobile.ui.ATSMobileApp
import com.riva.atsmobile.ui.theme.ATSMobileTheme
import com.riva.atsmobile.utils.NetworkMonitor
import com.riva.atsmobile.utils.NetworkUtils
import com.riva.atsmobile.viewmodel.SelectionViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        private const val PERM_REQUEST_LOCATION = 1001
    }

    private val viewModel: SelectionViewModel by viewModels()

    // 1) Launcher pour la permission VPN
    val requestVpnPermission =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Démarre votre service VPN intégré
                NetworkUtils.startAlwaysOnVpn(this)
            } else {
                Toast.makeText(this, "Permission VPN refusée", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 2) Permission localisation
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERM_REQUEST_LOCATION
            )
        } else {
            // Vérifier et lancer VPN si besoin
            NetworkUtils.verifierConnexionEtEventuellementLancerVpn(this)
        }

        // Observateur réseau
        NetworkMonitor.register(applicationContext)

        // UI system bars
        WindowCompat.setDecorFitsSystemWindows(window, true)
        lifecycleScope.launch {
            viewModel.role.collectLatest { role ->
                if (role.equals("ADMIN", ignoreCase = true)) {
                    WindowInsetsControllerCompat(window, window.decorView)
                        .show(WindowInsetsCompat.Type.statusBars())
                } else {
                    WindowInsetsControllerCompat(window, window.decorView).apply {
                        hide(WindowInsetsCompat.Type.statusBars())
                        systemBarsBehavior =
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                }
            }
        }

        setContent {
            ATSMobileTheme {
                ATSMobileApp(viewModel = viewModel)
            }
        }
    }

    // 3) Résultat permission localisation
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERM_REQUEST_LOCATION) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                NetworkUtils.verifierConnexionEtEventuellementLancerVpn(this)
            } else {
                Toast.makeText(
                    this,
                    "La permission de localisation est requise pour vérifier le réseau Wi-Fi",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
