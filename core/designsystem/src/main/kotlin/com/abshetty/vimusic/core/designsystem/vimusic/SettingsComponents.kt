package com.abshetty.vimusic.core.designsystem.vimusic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsGroup(title: String, modifier: Modifier = Modifier) {
    val (colorPalette, typography) = LocalAppearance.current
    Text(
        text = title.uppercase(),
        style = typography.xxs.semiBold,
        color = colorPalette.accent,
        modifier = modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp),
    )
}

@Composable
fun SettingsDescription(text: String, modifier: Modifier = Modifier) {
    val (colorPalette, typography) = LocalAppearance.current
    Text(
        text = text,
        style = typography.xxs,
        color = colorPalette.textSecondary,
        modifier = modifier.padding(horizontal = 24.dp, vertical = 8.dp),
    )
}

@Composable
fun SettingsEntry(
    title: String,
    text: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val (colorPalette, typography) = LocalAppearance.current
    val titleColor = if (enabled) colorPalette.text else colorPalette.textDisabled
    val textColor = if (enabled) colorPalette.textSecondary else colorPalette.textDisabled

    Row(
        modifier
            .fillMaxWidth()
            .then(
                if (onClick != null && enabled) Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = typography.xs.semiBold, color = titleColor)
            text?.let {
                Text(it, style = typography.xs.semiBold, color = textColor)
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun SettingsSwitch(
    title: String,
    text: String?,
    checked: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val (colorPalette) = LocalAppearance.current
    SettingsEntry(
        title = title,
        text = text,
        modifier = modifier,
        enabled = enabled,

        onClick = { onCheckedChange(!checked) },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colorPalette.onAccent,
                    checkedTrackColor = colorPalette.accent,
                    uncheckedThumbColor = colorPalette.textSecondary,
                    uncheckedTrackColor = colorPalette.background2,
                    uncheckedBorderColor = colorPalette.background2,
                ),
            )
        },
    )
}

@Composable
fun SettingsWarning(text: String, modifier: Modifier = Modifier) {
    val (colorPalette, typography) = LocalAppearance.current
    Text(
        text = text,
        style = typography.xxs.semiBold,
        color = colorPalette.red,
        modifier = modifier.padding(horizontal = 24.dp, vertical = 8.dp),
    )
}

@Composable
fun SettingsSpacer(height: Int = 16) = Column(Modifier.height(height.dp)) {}
