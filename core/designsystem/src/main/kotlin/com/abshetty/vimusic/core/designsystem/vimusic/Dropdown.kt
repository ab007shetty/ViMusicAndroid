package com.abshetty.vimusic.core.designsystem.vimusic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun SettingsDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val (colorPalette, typography) = LocalAppearance.current
    var expanded by remember { mutableStateOf(false) }

    Column(modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(
            label,
            style = typography.xxs.semiBold,
            color = colorPalette.textSecondary,
            modifier = Modifier.padding(bottom = 6.dp),
        )

        Box {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colorPalette.background2)
                    .clickable(enabled = enabled) { expanded = true }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    selected,
                    style = typography.xs.semiBold,
                    color = if (enabled) colorPalette.text else colorPalette.textDisabled,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = colorPalette.textSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = colorPalette.background1,

                modifier = Modifier.heightIn(max = MenuMaxHeight),
            ) {
                options.forEachIndexed { index, option ->
                    val isSelected = option == selected
                    Row(
                        Modifier
                            .clickable { onSelect(index); expanded = false }
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = if (isSelected) colorPalette.accent else Color.Transparent,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            option,
                            style = typography.xs.semiBold,
                            color = if (isSelected) colorPalette.accent else colorPalette.text,
                        )
                    }
                }
            }
        }
    }
}

private val MenuMaxHeight = 320.dp
