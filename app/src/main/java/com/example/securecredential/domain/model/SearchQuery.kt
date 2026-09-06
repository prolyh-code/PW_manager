package com.example.securecredential.domain.model

/** Spec section 8.4 — Exact Search only for MVP (Partial Search is P1, spec 8.3). */
sealed interface SearchQuery {
    data class Text(val value: String) : SearchQuery
    data class Domain(val value: String) : SearchQuery
    data class Url(val value: String) : SearchQuery
}
