package com.example.securecredential.presentation.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.securecredential.data.security.KeyMaterialStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Spec 13.2: "First Run" vs "Not First Run" is decided by whether key material already exists. */
enum class LaunchDestination { FIRST_LAUNCH, AUTHENTICATION }

@HiltViewModel
class LaunchViewModel @Inject constructor(
    private val keyMaterialStore: KeyMaterialStore
) : ViewModel() {

    private val _destination = MutableStateFlow<LaunchDestination?>(null)
    val destination: StateFlow<LaunchDestination?> = _destination.asStateFlow()

    init {
        viewModelScope.launch {
            val hasKeys = keyMaterialStore.snapshot().localWrappedAppKey != null
            _destination.value = if (hasKeys) LaunchDestination.AUTHENTICATION else LaunchDestination.FIRST_LAUNCH
        }
    }
}
