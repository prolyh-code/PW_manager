package com.example.securecredential.presentation.category

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.securecredential.R
import com.example.securecredential.presentation.common.BackButton
import com.example.securecredential.presentation.common.CredentialSummaryRow

/** Spec 13.2 "Category -> Credential List -> Credential Detail". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialListScreen(
    onBack: () -> Unit,
    onCredentialClick: (String) -> Unit,
    onAddCredential: () -> Unit,
    viewModel: CredentialListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Same fix as HomeScreen: the ViewModel survives navigating to Add/Edit and back, so the
    // list must refresh on every re-visit, not just once in ViewModel init.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_all_credentials)) },
                navigationIcon = { BackButton(onClick = onBack) }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                uiState.all.isEmpty() -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(R.string.empty_no_credentials),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onAddCredential) { Text(stringResource(R.string.action_add_first_credential)) }
                }
                else -> LazyColumn(modifier = Modifier.padding(16.dp)) {
                    items(uiState.all, key = { it.credentialId }) { summary ->
                        CredentialSummaryRow(summary, onClick = { onCredentialClick(summary.credentialId) })
                    }
                }
            }

            uiState.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
            }
        }
    }
}
