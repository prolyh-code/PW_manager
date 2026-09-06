package com.example.securecredential.presentation.authentication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Shared numeric keypad reused by Security Setup, PIN change, and Restore's PIN entry (spec 13.3). */
@Composable
fun PinKeypad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9')
    )
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { digit -> DigitButton(digit, onClick = { onDigit(digit) }) }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Spacer(modifier = Modifier.size(64.dp))
            DigitButton('0', onClick = { onDigit('0') })
            IconButton(onClick = onBackspace, modifier = Modifier.size(64.dp)) {
                Text("⌫", style = MaterialTheme.typography.headlineSmall) // erase symbol
            }
        }
    }
}

@Composable
private fun DigitButton(digit: Char, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.size(64.dp)) {
        Text(digit.toString(), style = MaterialTheme.typography.headlineSmall)
    }
}

/** Progress dots for PIN entry — fills as characters are typed, un-fills otherwise. */
@Composable
fun PinDotsIndicator(enteredLength: Int, minLength: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(maxOf(enteredLength, minLength)) { index ->
            val filled = index < enteredLength
            val dotModifier = Modifier
                .size(12.dp)
                .let {
                    if (filled) {
                        it.background(MaterialTheme.colorScheme.primary, CircleShape)
                    } else {
                        it.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    }
                }
            androidx.compose.foundation.layout.Box(modifier = dotModifier)
        }
    }
}
