package com.riva.atsmobile.utils

import android.widget.Toast
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import com.riva.atsmobile.viewmodel.SelectionViewModel

/**
 * Texte version cliquable qui active le mode développeur après 7 clics.
 *
 * @param viewModel Le ViewModel contenant le flag `devModeEnabled`
 * @param versionText Texte de version à afficher
 */
@Composable
fun DevModeActivator(viewModel: SelectionViewModel, versionText: String) {
    var tapCount by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val isDevActive by viewModel.devModeEnabled.collectAsState()

    Text(
        text = "Version : $versionText",
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .padding(top = 4.dp)
            .clickable {
                if (!isDevActive) {
                    tapCount++
                    if (tapCount >= 7) {
                        viewModel.activateDevMode()
                        Toast.makeText(context, "🧪 Mode développeur activé", Toast.LENGTH_SHORT).show()
                        tapCount = 0
                    }
                }
            }
    )
}
