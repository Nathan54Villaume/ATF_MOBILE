package com.riva.atsmobile.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riva.atsmobile.model.Etape
import com.riva.atsmobile.network.ApiAutomateClient
import com.riva.atsmobile.utils.ApiConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EtapeViewModel : ViewModel() {

    private val _etapes = MutableStateFlow<List<Etape>>(emptyList())
    val etapes: StateFlow<List<Etape>> = _etapes

    fun loadEtapes(context: Context) {
        viewModelScope.launch {
            try {
                val baseUrl = ApiConfig.getBaseUrl(context)
                val result = ApiAutomateClient.getEtapes(baseUrl)
                _etapes.value = result
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}
