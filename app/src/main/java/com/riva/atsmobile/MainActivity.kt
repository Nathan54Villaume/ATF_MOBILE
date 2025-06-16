package com.riva.atsmobile
import androidx.lifecycle.lifecycleScope
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.riva.atsmobile.ui.ATSMobileApp
import com.riva.atsmobile.ui.theme.ATSMobileTheme
import com.riva.atsmobile.utils.NetworkUtils
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

        // 1. Demande de permission de localisation si nécessaire
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERM_REQUEST_LOCATION
            )
        } else {
            // permission déjà accordée → on peut vérifier la connexion VPN
            NetworkUtils.verifierConnexionEtEventuellementLancerVpn(this)
        }

        // Enregistre l’observateur réseau réactif (ne déclenche plus le VPN ici)
        NetworkMonitor.register(applicationContext)

        // Configuration des barres système
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

        // Interface principale
        setContent {
            ATSMobileTheme {
                ATSMobileApp(viewModel = viewModel)
            }
        }
    }

    // Gère la réponse de la demande de permission
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERM_REQUEST_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission accordée → vérifier le VPN
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
