package com.example.securecredential.presentation.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.securecredential.R
import com.example.securecredential.presentation.common.BackButton
import com.example.securecredential.presentation.common.CredentialSummaryRow

/** Spec 13.3: card list — Service/Domain/ID Mask/Password Mask only, never a plaintext password. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultScreen(
    onCredentialClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: SearchResultViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search_result_title_format, uiState.query)) },
                navigationIcon = { BackButton(onClick = onBack) }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                uiState.isSearching -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                uiState.results.isEmpty() -> Text(
                    stringResource(R.string.search_no_exact_match),
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
                else -> Column(modifier = Modifier.padding(16.dp)) {
                    LazyColumn {
                        items(uiState.results, key = { it.credentialId }) { summary ->
                            CredentialSummaryRow(summary, onClick = { onCredentialClick(summary.credentialId) })
                        }
                    }
                }
            }
        }
    }
}
