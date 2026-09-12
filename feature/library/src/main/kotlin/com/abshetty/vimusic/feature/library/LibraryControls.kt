package com.abshetty.vimusic.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.abshetty.vimusic.core.designsystem.vimusic.LocalAppearance
import com.abshetty.vimusic.core.designsystem.vimusic.medium
import com.abshetty.vimusic.core.designsystem.vimusic.semiBold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(
    query: String,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val (colorPalette, typography) = LocalAppearance.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colorPalette.background1,
    ) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text(
                "Filter",
                style = typography.xs.semiBold,
                color = colorPalette.textSecondary,
                modifier = Modifier.padding(start = 24.dp, bottom = 8.dp),
            )

            Row(
                Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colorPalette.background2)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = null,
                    tint = colorPalette.textDisabled,
                    modifier = Modifier.size(18.dp),
                )
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            "Search this list",
                            style = typography.xs,
                            color = colorPalette.textDisabled,
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = typography.xs.copy(color = colorPalette.text),
                        cursorBrush = SolidColor(colorPalette.accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (query.isNotEmpty()) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Clear",
                        tint = colorPalette.textSecondary,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onQueryChange("") },
                    )
                }
            }
        }
    }
}
