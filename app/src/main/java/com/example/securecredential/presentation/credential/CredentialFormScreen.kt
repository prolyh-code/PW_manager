package com.example.securecredential.presentation.credential

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/** Spec 13.3 Credential Registration/Edit form. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialFormScreen(
    onSaved: () -> Unit,
    onCancel: () -> Unit,
    viewModel: CredentialFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onSaved()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(if (uiState.isEditing) "Edit Credential" else "Add Credential") }) }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = uiState.url,
                onValueChange = viewModel::onUrlChange,
                label = { Text("URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            SpacerM()
            OutlinedTextField(
                value = uiState.serviceName,
                onValueChange = viewModel::onServiceNameChange,
                label = { Text("Service Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            SpacerM()
            Box {
                OutlinedTextField(
                    value = uiState.username,
                    onValueChange = viewModel::onUsernameChange,
                    label = { Text("ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                UsernameAutocompleteDropdown(
                    suggestions = uiState.usernameSuggestions,
                    currentUsername = uiState.username,
                    onSelect = viewModel::applyUsernameSuggestion,
                    onDismiss = { viewModel.applyUsernameSuggestion(uiState.username) }
                )
            }
            SpacerM()
            OutlinedTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            SpacerM()
            OutlinedTextField(
                value = uiState.usernameMask,
                onValueChange = viewModel::onUsernameMaskChange,
                label = { Text("ID Mask (how it appears in search results)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            SpacerM()
            OutlinedTextField(
                value = uiState.passwordMask,
                onValueChange = viewModel::onPasswordMaskChange,
                label = { Text("Password Mask (how it appears in search results)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            SpacerM()
            OutlinedTextField(
                value = uiState.category,
                onValueChange = viewModel::onCategoryChange,
                label = { Text("Category") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            SpacerM()
            OutlinedTextField(
                value = uiState.memo,
                onValueChange = viewModel::onMemoChange,
                label = { Text("Memo") },
                modifier = Modifier.fillMaxWidth()
            )

            SpacerL()
            Button(
                onClick = viewModel::save,
                enabled = !uiState.isSaving && uiState.serviceName.isNotBlank() && uiState.username.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isSaving) CircularProgressIndicator(modifier = Modifier.padding(2.dp)) else Text("Save")
            }

            uiState.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp))
            }
        }

        if (uiState.showReuseWarning) {
            PasswordReuseWarningDialog(
                onDismiss = viewModel::dismissReuseWarning,
                onContinueAnyway = viewModel::dismissReuseWarning
            )
        }
    }
}

@Composable
private fun SpacerM() = Spacer(Modifier.height(12.dp))

@Composable
private fun SpacerL() = Spacer(Modifier.height(24.dp))
