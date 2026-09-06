package com.example.securecredential.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Spec 13.3: "Avatar는 서비스 로고 자동 다운로드 없이 서비스명 첫 글자 기반으로 생성" — no network fetch.
private val AvatarPalette = listOf(
    Color(0xFF6C5CE7), Color(0xFFE17055), Color(0xFF00B894),
    Color(0xFF0984E3), Color(0xFFD63384), Color(0xFFE8A33D), Color(0xFF6D5B98)
)

@Composable
fun ServiceAvatar(serviceName: String, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 48.dp) {
    val letter = serviceName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val color = AvatarPalette[avatarColorIndex(serviceName)]
    val onColor = if (color.luminance() > 0.5f) Color.Black else Color.White

    Box(
        modifier = modifier.size(size).background(color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(letter, color = onColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
    }
}

private fun avatarColorIndex(serviceName: String): Int {
    if (serviceName.isEmpty()) return 0
    // Stable per name (not per-render-random) so the same service always gets the same color.
    val hash = serviceName.trim().lowercase().sumOf { it.code }
    return hash % AvatarPalette.size
}
