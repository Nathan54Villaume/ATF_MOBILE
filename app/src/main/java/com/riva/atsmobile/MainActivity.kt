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
import androidx.lifecycle.lifecycleScope
import com.riva.atsmobile.ui.ATSMobileApp
import com.riva.atsmobile.ui.theme.ATSMobileTheme
import com.riva.atsmobile.utils.NetworkMonitor
import com.riva.atsmobile.viewmodel.SelectionViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        private const val PERM_REQUEST_LOCATION = 1001
    }

    private val viewModel: SelectionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) Demande de permission de localisation si nécessaire
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERM_REQUEST_LOCATION
            )
        }

        // 2) Démarrage de l'observateur réseau (pour UI offline/online, etc.)
        NetworkMonitor.register(applicationContext)

        // 3) Configuration des barres système
        WindowCompat.setDecorFitsSystemWindows(window, true)
        lifecycleScope.launch {
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

        // 4) Mise en place de Compose
        setContent {
            ATSMobileTheme {
                ATSMobileApp(viewModel = viewModel)
            }
        }
    }

    // Traitement du résultat de la demande de permission
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERM_REQUEST_LOCATION && grantResults.firstOrNull() != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(
                this,
                "La permission de localisation est requise pour certaines fonctionnalités réseau.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
