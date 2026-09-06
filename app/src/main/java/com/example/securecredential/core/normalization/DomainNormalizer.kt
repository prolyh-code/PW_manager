package com.example.securecredential.core.normalization

/**
 * Domain/URL normalization per spec 8.1. Subdomains are deliberately preserved
 * (accounts.google.com != google.com, spec's own example) — this is NOT eTLD+1 reduction.
 */
object DomainNormalizer {

    fun normalizeDomain(input: String): String {
        var value = input.trim().lowercase()
        value = value.removePrefix("https://").removePrefix("http://")
        value = value.removePrefix("www.")
        // Strip fragment/query/path in that order (they nest: authority/path?query#fragment),
        // then strip a port off what remains of the host.
        value = value.substringBefore('#').substringBefore('?').substringBefore('/')
        value = value.substringBefore(':')
        return value.trimEnd('/')
    }

    /** Spec 8.4's SearchQuery.Url path reuses the same host-extraction rules. */
    fun extractDomainFromUrl(url: String): String = normalizeDomain(url)

    /** Spec 8.1 example: "accounts.google.com" -> "Google" (a suggestion the user can edit). */
    fun suggestServiceName(normalizedDomain: String): String {
        val labels = normalizedDomain.split('.').filter { it.isNotEmpty() }
        val label = when {
            labels.size >= 2 -> labels[labels.size - 2]
            labels.isNotEmpty() -> labels[0]
            else -> return ""
        }
        return label.replaceFirstChar { it.uppercase() }
    }
}
