package com.abshetty.vimusic.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abshetty.vimusic.core.designsystem.vimusic.HapticSlider
import com.abshetty.vimusic.core.designsystem.vimusic.LocalAppearance
import com.abshetty.vimusic.core.designsystem.vimusic.RailIconButton
import com.abshetty.vimusic.core.designsystem.vimusic.SettingsDropdown
import com.abshetty.vimusic.core.designsystem.vimusic.semiBold
import com.abshetty.vimusic.core.media.audio.ReverbPreset
import kotlin.math.roundToInt

private val SCREEN_PADDING = 20.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: EqualizerViewModel = hiltViewModel(),
) {
    val fx by viewModel.state.collectAsStateWithLifecycle()

    val (colorPalette, typography) = LocalAppearance.current

    Column(
        modifier
            .fillMaxSize()
            .background(colorPalette.background0)
            .verticalScroll(rememberScrollState())
            .padding(
                start = SCREEN_PADDING,
                end = SCREEN_PADDING,
                top = 16.dp,
                bottom = 40.dp,
            )
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            RailIconButton(
                icon = Icons.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = colorPalette.text,
                onClick = onBack,
            )

            Text(
                "Equalizer",
                style = typography.l.semiBold,
                color = colorPalette.text,
                modifier = Modifier.weight(1f).padding(start = 12.dp),
            )
            Switch(
                checked = fx.enabled,
                onCheckedChange = viewModel::setEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colorPalette.onAccent,
                    checkedTrackColor = colorPalette.accent,
                    uncheckedThumbColor = colorPalette.textSecondary,
                    uncheckedTrackColor = colorPalette.background2,
                    uncheckedBorderColor = colorPalette.background2,
                ),
            )
        }

        Gap(20)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsDropdown(
                label = "Preset",
                selected = fx.presetLabel,
                options = fx.presets,
                onSelect = viewModel::usePreset,
                modifier = Modifier.weight(1f).padding(horizontal = 0.dp),
            )
            if (fx.hasReverb) {
                SettingsDropdown(
                    label = "Reverb",
                    selected = ReverbPreset.entries
                        .firstOrNull { it.value == fx.reverb }?.label ?: "Off",
                    options = ReverbPreset.entries.map { it.label },
                    onSelect = { viewModel.setReverb(ReverbPreset.entries[it].value) },
                    modifier = Modifier.weight(1f).padding(horizontal = 0.dp),
                )
            }
        }

        Gap(24)

        SectionHeader("Bands") {
            TextAction("Flat", onClick = viewModel::resetBands)
        }

        Bands(
            bands = fx.bands,
            minGainDb = fx.minGainDb,
            maxGainDb = fx.maxGainDb,
            enabled = fx.enabled,
            onChange = viewModel::setBand,
        )

        Gap(8)

        DbSlider(
            label = "Preamp",
            valueDb = fx.preampDb,
            range = -12f..12f,
            enabled = fx.enabled,
            onChange = viewModel::setPreamp,
        )

        Gap(24)
        SectionHeader("Tone")

        PercentSlider("Bass boost", fx.bassBoost, 0..1000, fx.enabled, viewModel::setBassBoost)
        PercentSlider(
            "Stereo width", fx.virtualizer, 0..1000, fx.enabled, viewModel::setVirtualizer,
        )
        PercentSlider("Volume gain", fx.loudnessMb, 0..1200, fx.enabled, viewModel::setLoudness)

        Gap(24)
        SectionHeader("Stereo")

        SwitchRow(
            label = "Mono",
            description = "Both channels carry the same sound. For one earbud, " +
                "a single speaker, or an old recording split across the two sides.",
            checked = fx.mono,
            enabled = fx.enabled,
            onChange = viewModel::setMono,
        )

        BalanceSlider(
            value = fx.balance,
            enabled = fx.enabled,
            onChange = viewModel::setBalance,
        )

        Gap(24)
        SectionHeader("Playback")
        Text(
            "Speed and pitch are independent, so a track can be slowed down " +
                "without dropping an octave. Double-tap a dial to reset it.",
            style = typography.xxs,
            color = colorPalette.textSecondary,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            SpeedDial(
                value = fx.speed,
                onValueChange = viewModel::setSpeed,
                range = 0.5f..2.0f,
                label = "Speed",
            )
            SpeedDial(
                value = fx.pitch,
                onValueChange = viewModel::setPitch,
                range = 0.5f..2.0f,
                label = "Pitch",
            )
        }

        Gap(24)
    }
}

@Composable
private fun Gap(dp: Int) = Box(Modifier.height(dp.dp))

@Composable
private fun SwitchRow(
    label: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val (colorPalette, typography) = LocalAppearance.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                label,
                style = typography.xs,
                color = if (enabled) colorPalette.text else colorPalette.textDisabled,
            )
            Text(description, style = typography.xxs, color = colorPalette.textSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colorPalette.onAccent,
                checkedTrackColor = colorPalette.accent,
                uncheckedThumbColor = colorPalette.textSecondary,
                uncheckedTrackColor = colorPalette.background2,
                uncheckedBorderColor = colorPalette.background2,
            ),
        )
    }
}

@Composable
private fun BalanceSlider(value: Float, enabled: Boolean, onChange: (Float) -> Unit) {
    val (colorPalette, typography) = LocalAppearance.current
    Column(Modifier.padding(top = 8.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(
                "Balance",
                style = typography.xs,
                color = if (enabled) colorPalette.text else colorPalette.textDisabled,
                modifier = Modifier.weight(1f),
            )
            Text(
                when {
                    value < -0.005f -> "L " + (-value * 100).roundToInt() + "%"
                    value > 0.005f -> "R " + (value * 100).roundToInt() + "%"
                    else -> "Centre"
                },
                style = typography.xxs.semiBold,
                color = colorPalette.textSecondary,
            )
        }
        HapticSlider(
            value = value,
            onValueChange = onChange,
            valueRange = -1f..1f,
            enabled = enabled,
            stepSize = 0.05f,
        )
    }
}

@Composable
private fun SectionHeader(title: String, action: @Composable (() -> Unit)? = null) {
    val (colorPalette, typography) = LocalAppearance.current
    Row(
        Modifier.fillMaxWidth().padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = typography.xs.semiBold,
            color = colorPalette.textSecondary,
            modifier = Modifier.weight(1f),
        )
        action?.invoke()
    }
}

@Composable
private fun TextAction(label: String, onClick: () -> Unit) {
    val (colorPalette, typography) = LocalAppearance.current
    Text(
        label,
        style = typography.xxs.semiBold,
        color = colorPalette.text,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colorPalette.background2)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

@Composable
private fun Bands(
    bands: List<com.abshetty.vimusic.core.media.audio.Band>,
    minGainDb: Float,
    maxGainDb: Float,
    enabled: Boolean,
    onChange: (Int, Float) -> Unit,
) {
    val (colorPalette, typography) = LocalAppearance.current

    Row(
        Modifier.fillMaxWidth().height(240.dp).padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        bands.forEach { band ->
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(

                    (if (band.gainDb > 0) "+" else "") + band.gainDb.roundToInt(),
                    style = typography.xxs.semiBold,
                    color = if (band.gainDb == 0f) colorPalette.textDisabled
                    else colorPalette.accent,
                )
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    VerticalSlider(
                        value = band.gainDb,
                        onValueChange = { onChange(band.index, it) },
                        valueRange = minGainDb..maxGainDb,
                        enabled = enabled,
                    )
                }
                Text(
                    band.label,
                    style = typography.xxs,
                    color = colorPalette.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun DbSlider(
    label: String,
    valueDb: Float,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onChange: (Float) -> Unit,
) {
    val (colorPalette, typography) = LocalAppearance.current
    Column(Modifier.padding(top = 8.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(
                label,
                style = typography.xs,
                color = if (enabled) colorPalette.text else colorPalette.textDisabled,
                modifier = Modifier.weight(1f),
            )
            Text(
                (if (valueDb > 0) "+" else "") + valueDb.roundToInt() + " dB",
                style = typography.xxs.semiBold,
                color = colorPalette.textSecondary,
            )
        }
        HapticSlider(
            value = valueDb,
            onValueChange = onChange,
            valueRange = range,
            enabled = enabled,
            stepSize = 1f,
        )
    }
}

@Composable
private fun PercentSlider(
    label: String,
    value: Int,
    range: IntRange,
    enabled: Boolean,
    onChange: (Int) -> Unit,
) {
    val (colorPalette, typography) = LocalAppearance.current
    Column(Modifier.padding(top = 8.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(
                label,
                style = typography.xs,
                color = if (enabled) colorPalette.text else colorPalette.textDisabled,
                modifier = Modifier.weight(1f),
            )
            Text(
                ((value - range.first) * 100 / (range.last - range.first)).toString() + "%",
                style = typography.xxs.semiBold,
                color = colorPalette.textSecondary,
            )
        }
        HapticSlider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            enabled = enabled,

            stepSize = (range.last - range.first) / 10f,
        )
    }
}

@Composable
private fun VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
) {
    Layout(
        content = {
            HapticSlider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                enabled = enabled,

                stepSize = 1f,
                modifier = Modifier.graphicsLayer { rotationZ = 270f },
            )
        },
    ) { measurables, constraints ->

        val placeable = measurables.first().measure(
            constraints.copy(
                minWidth = constraints.minHeight,
                maxWidth = constraints.maxHeight,
                minHeight = constraints.minWidth,
                maxHeight = constraints.maxWidth,
            )
        )
        layout(placeable.height, placeable.width) {
            placeable.place(
                x = -(placeable.width / 2 - placeable.height / 2),
                y = -(placeable.height / 2 - placeable.width / 2),
            )
        }
    }
}
