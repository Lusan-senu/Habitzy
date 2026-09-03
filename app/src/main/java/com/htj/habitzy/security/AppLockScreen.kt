package com.htj.habitzy.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.htj.habitzy.ui.theme.SpaceL
import com.htj.habitzy.ui.theme.SpaceXL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockScreen(
    storedPinHash: String,
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Habitzy",
    dismissible: Boolean = false,
    onDismiss: (() -> Unit)? = null,
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit(attempt: String) {
        if (PinHasher.hash(attempt) == storedPinHash) {
            pin = ""
            onUnlocked()
        } else {
            pin = ""
            error = "Incorrect PIN"
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpaceXL),
        ) {
            if (dismissible && onDismiss != null) {
                Row(Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Skip") }
                }
            }
            Spacer(Modifier.height(SpaceXL))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(SpaceL))
            Text(
                text = "Enter your PIN",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(SpaceL))

            Row(
                horizontalArrangement = Arrangement.spacedBy(SpaceL),
                modifier = Modifier.padding(vertical = 16.dp),
            ) {
                repeat(4) { index ->
                    Box(
                        modifier = Modifier
                            .size(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            modifier = Modifier.size(16.dp),
                            shape = CircleShape,
                            color = if (index < pin.length) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                        ) {}
                    }
                }
            }

            if (error != null) {
                Text(
                    text = error ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(SpaceL))

            Keypad(
                onDigit = { d ->
                    error = null
                    if (pin.length < 4) {
                        val next = pin + d
                        pin = next
                        if (next.length == 4) submit(next)
                    }
                },
                onBackspace = { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
                enabled = true,
            )
        }
    }
}

@Composable
private fun Keypad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    enabled: Boolean,
) {
    val digits = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫")
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        digits.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                row.forEach { label ->
                    when {
                        label == "" -> Box(Modifier.size(72.dp))
                        label == "⌫" -> KeypadButton(
                            label = "⌫",
                            enabled = enabled,
                            fontSize = 20.sp,
                            onClick = onBackspace,
                        )
                        else -> KeypadButton(label = label, enabled = enabled) { onDigit(label) }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun KeypadButton(
    label: String,
    enabled: Boolean,
    fontSize: androidx.compose.ui.unit.TextUnit = 24.sp,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.size(72.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = fontSize,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
