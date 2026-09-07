package com.example.securecredential.presentation.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.securecredential.R
import com.example.securecredential.presentation.common.CredentialSummaryRow

/** Spec 13.2/13.3: search-first Home, Bottom Nav (Home/Category/Settings), FAB to add. */
@Composable
fun HomeScreen(
    onCredentialClick: (String) -> Unit,
    onSearchSubmit: (String) -> Unit,
    onAddCredential: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // The ViewModel survives navigating away and back (e.g. Add Credential -> Save -> back to
    // Home), so `recent` must be refreshed whenever this destination becomes visible again, not
    // just once in ViewModel init — otherwise a newly-saved credential never appears until the
    // app is killed and restarted.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadRecent()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddCredential) { Text(stringResource(R.string.action_add)) }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = true, onClick = {}, icon = {}, label = { Text(stringResource(R.string.nav_home)) })
                NavigationBarItem(selected = false, onClick = onOpenCategories, icon = {}, label = { Text(stringResource(R.string.nav_category)) })
                NavigationBarItem(selected = false, onClick = onOpenSettings, icon = {}, label = { Text(stringResource(R.string.nav_settings)) })
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                label = { Text(stringResource(R.string.home_search_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    if (uiState.query.isNotBlank()) onSearchSubmit(uiState.query.trim())
                }),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            if (uiState.query.isNotBlank()) {
                // Live, per-character autocomplete — client-side filter over the already-
                // decrypted list this screen holds (see HomeViewModel.onQueryChange for why
                // this doesn't touch the encrypted search index).
                Text(
                    stringResource(R.string.section_suggestions), style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (uiState.suggestions.isEmpty()) {
                    Text(stringResource(R.string.home_no_matches))
                } else {
                    LazyColumn {
                        items(uiState.suggestions, key = { it.credentialId }) { summary ->
                            CredentialSummaryRow(summary, onClick = { onCredentialClick(summary.credentialId) })
                        }
                    }
                }
            } else {
                Text(stringResource(R.string.section_recent), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
                LazyColumn {
                    items(uiState.recent, key = { it.credentialId }) { summary ->
                        CredentialSummaryRow(summary, onClick = { onCredentialClick(summary.credentialId) })
                    }
                }
            }

            uiState.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp))
            }
        }
    }
}
