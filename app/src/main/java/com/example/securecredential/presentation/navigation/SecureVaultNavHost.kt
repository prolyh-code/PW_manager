package com.example.securecredential.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.securecredential.presentation.authentication.AuthenticationScreen
import com.example.securecredential.presentation.authentication.FirstLaunchScreen
import com.example.securecredential.presentation.authentication.LaunchDestination
import com.example.securecredential.presentation.authentication.LaunchViewModel
import com.example.securecredential.presentation.authentication.SecuritySetupScreen
import com.example.securecredential.presentation.backup.BackupRestoreScreen
import com.example.securecredential.presentation.category.CredentialListScreen
import com.example.securecredential.presentation.credential.CredentialDetailScreen
import com.example.securecredential.presentation.credential.CredentialFormScreen
import com.example.securecredential.presentation.home.HomeScreen
import com.example.securecredential.presentation.search.SearchResultScreen
import com.example.securecredential.presentation.settings.SettingsScreen

/** Navigation shell per spec 13.2 — every destination is wired to a real screen. */
@Composable
fun SecureVaultNavHost(navController: NavHostController = rememberNavController()) {
    Scaffold { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Launch.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.Launch.route) {
                val viewModel: LaunchViewModel = hiltViewModel()
                val destination by viewModel.destination.collectAsState()

                LaunchedEffect(destination) {
                    val target = when (destination) {
                        LaunchDestination.FIRST_LAUNCH -> Routes.FirstLaunch.route
                        LaunchDestination.AUTHENTICATION -> Routes.Authentication.route
                        null -> return@LaunchedEffect
                    }
                    navController.navigate(target) {
                        popUpTo(Routes.Launch.route) { inclusive = true }
                    }
                }

                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            composable(Routes.FirstLaunch.route) {
                FirstLaunchScreen(onGetStarted = { navController.navigate(Routes.SecuritySetup.route) })
            }

            composable(Routes.SecuritySetup.route) {
                SecuritySetupScreen(
                    onSetupComplete = {
                        navController.navigate(Routes.Home.route) {
                            popUpTo(Routes.Launch.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.Authentication.route) {
                AuthenticationScreen(
                    onUnlocked = {
                        navController.navigate(Routes.Home.route) {
                            popUpTo(Routes.Launch.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.Home.route) {
                HomeScreen(
                    onCredentialClick = { id -> navController.navigate(Routes.CredentialDetail.createRoute(id)) },
                    onSearchSubmit = { query -> navController.navigate(Routes.SearchResult.createRoute(query)) },
                    onAddCredential = { navController.navigate(Routes.CredentialForm.createRouteForNew()) },
                    onOpenCategories = { navController.navigate(Routes.CredentialList.route) },
                    onOpenSettings = { navController.navigate(Routes.Settings.route) }
                )
            }

            composable(
                Routes.SearchResult.route,
                arguments = listOf(navArgument("query") { type = NavType.StringType })
            ) {
                SearchResultScreen(
                    onCredentialClick = { id -> navController.navigate(Routes.CredentialDetail.createRoute(id)) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.CredentialList.route) {
                CredentialListScreen(
                    onBack = { navController.popBackStack() },
                    onCredentialClick = { id -> navController.navigate(Routes.CredentialDetail.createRoute(id)) },
                    onAddCredential = { navController.navigate(Routes.CredentialForm.createRouteForNew()) }
                )
            }

            composable(
                Routes.CredentialDetail.route,
                arguments = listOf(navArgument("credentialId") { type = NavType.StringType })
            ) {
                CredentialDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(Routes.CredentialForm.createRouteForEdit(id)) }
                )
            }

            composable(
                Routes.CredentialForm.route,
                arguments = listOf(navArgument("credentialId") { type = NavType.StringType; defaultValue = Routes.CredentialForm.NEW })
            ) {
                CredentialFormScreen(
                    onSaved = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }

            composable(Routes.Settings.route) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenBackupRestore = { navController.navigate(Routes.BackupRestore.route) },
                    onLockedOut = {
                        navController.navigate(Routes.Authentication.route) {
                            popUpTo(Routes.Launch.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.BackupRestore.route) {
                BackupRestoreScreen(
                    onBack = { navController.popBackStack() },
                    onRestoreComplete = {
                        navController.navigate(Routes.Home.route) {
                            popUpTo(Routes.Launch.route) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TodoScreen(name: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("TODO: $name")
    }
}
