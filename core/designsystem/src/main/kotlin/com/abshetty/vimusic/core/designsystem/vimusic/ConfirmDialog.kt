package com.abshetty.vimusic.core.designsystem.vimusic

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ConfirmDialog(
    title: String,
    consequence: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    destructive: Boolean = true,
) {
    val (colorPalette, typography) = LocalAppearance.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colorPalette.background1,
        title = { Text(title, style = typography.s.semiBold, color = colorPalette.text) },
        text = {
            Column {
                Text(
                    consequence,
                    style = typography.xs,
                    color = colorPalette.textSecondary,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(
                    confirmLabel,
                    style = typography.xs.semiBold,
                    color = if (destructive) colorPalette.red else colorPalette.accent,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", style = typography.xs.semiBold, color = colorPalette.textSecondary)
            }
        },
    )
}
