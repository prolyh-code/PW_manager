package com.example.securecredential.presentation.navigation

/**
 * Route map per spec section 13.2. Screens are placeholder stubs until their
 * owning milestone (see plan file) implements them for real.
 */
sealed class Routes(val route: String) {
    /** Decides FirstLaunch vs Authentication based on whether key material already exists. */
    data object Launch : Routes("launch")
    data object FirstLaunch : Routes("first_launch")
    data object SecuritySetup : Routes("security_setup")
    data object Authentication : Routes("authentication")

    data object Home : Routes("home")
    data object SearchResult : Routes("search_result/{query}") {
        fun createRoute(query: String) = "search_result/${java.net.URLEncoder.encode(query, "UTF-8")}"
    }
    data object CredentialList : Routes("credential_list")
    data object CredentialDetail : Routes("credential_detail/{credentialId}") {
        fun createRoute(credentialId: String) = "credential_detail/$credentialId"
    }
    data object CredentialForm : Routes("credential_form?credentialId={credentialId}") {
        const val NEW = "new"
        fun createRouteForNew() = "credential_form?credentialId=$NEW"
        fun createRouteForEdit(credentialId: String) = "credential_form?credentialId=$credentialId"
    }

    data object Settings : Routes("settings")
    data object BackupRestore : Routes("backup_restore")
}
