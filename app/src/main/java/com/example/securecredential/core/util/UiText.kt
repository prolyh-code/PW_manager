package com.example.securecredential.core.util

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * Lets a ViewModel describe user-facing text without ever calling `Context.getString()` itself
 * (which (a) isn't reliably locale-correct on API < 33 outside an Activity/Composable context,
 * and (b) requires a real Android resource system, breaking plain-JVM unit tests). ViewModels
 * emit [UiText] — a resource id (+ format args) for known/static messages, or a raw string for
 * genuinely dynamic ones (e.g. an exception's own `.message`) — and only the Composable layer
 * resolves it via [asString], through the normal `stringResource()` mechanism that's always
 * guaranteed to reflect the current locale correctly.
 */
sealed interface UiText {
    data class Dynamic(val value: String) : UiText
    data class Resource(@StringRes val resId: Int, val args: List<Any> = emptyList()) : UiText

    companion object {
        fun of(@StringRes resId: Int, vararg args: Any): UiText = Resource(resId, args.toList())
    }
}

@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Dynamic -> value
    is UiText.Resource -> stringResource(resId, *args.toTypedArray())
}
