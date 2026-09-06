package com.example.securecredential

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.securecredential.data.preferences.ThemeMode
import com.example.securecredential.presentation.common.ThemeViewModel
import com.example.securecredential.presentation.common.theme.SecureVaultTheme
import com.example.securecredential.presentation.navigation.SecureVaultNavHost
import dagger.hilt.android.AndroidEntryPoint

// FragmentActivity (not bare ComponentActivity) because androidx.biometric.BiometricPrompt
// requires a FragmentActivity/Fragment host (data.security.BiometricAuthManager).
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val themeMode by themeViewModel.themeMode.collectAsState()
            val useDarkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            SecureVaultTheme(useDarkTheme = useDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SecureVaultNavHost()
                }
            }
        }
    }
}
