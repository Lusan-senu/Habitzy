package com.htj.habitzy.ui.settings.appearance

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.htj.habitzy.R
import com.htj.habitzy.data.local.datastore.AppIcon
import com.htj.habitzy.data.local.datastore.ThemeMode
import com.htj.habitzy.ui.addedithabit.CustomColorPicker
import com.htj.habitzy.ui.components.ColorSwatchPicker
import com.htj.habitzy.ui.components.SectionHeader
import com.htj.habitzy.ui.theme.SpaceL
import com.htj.habitzy.ui.theme.SpaceM
import com.htj.habitzy.ui.theme.SpaceS

private val ThemeModeOptions = listOf(
    ThemeMode.SYSTEM to "System",
    ThemeMode.LIGHT to "Light",
    ThemeMode.DARK to "Dark",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppearanceSettingsViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    var showCustomPicker by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Appearance") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader(title = "Theme mode")
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SpaceL),
            ) {
                ThemeModeOptions.forEachIndexed { index, (mode, label) ->
                    SegmentedButton(
                        selected = uiState.themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, ThemeModeOptions.size),
                    ) {
                        Text(label)
                    }
                }
            }

            SectionHeader(title = "Colors")
            ListItem(
                headlineContent = { Text("Dynamic color") },
                supportingContent = {
                    Text(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            "Use wallpaper colors from your device"
                        } else {
                            "Requires Android 12+"
                        },
                    )
                },
                trailingContent = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Switch(
                            checked = uiState.useDynamicColor,
                            onCheckedChange = viewModel::setUseDynamicColor,
                        )
                    }
                },
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !uiState.useDynamicColor) {
                AccentColorSection(
                    accentArgb = uiState.accentSeedArgb,
                    onPick = viewModel::setAccentSeed,
                    onShowCustom = { showCustomPicker = true },
                )
            }

            SectionHeader(title = "Background")
            ListItem(
                headlineContent = { Text("True black background") },
                supportingContent = { Text("Use pure black for OLED screens (dark mode)") },
                trailingContent = {
                    Switch(
                        checked = uiState.useTrueBlack,
                        onCheckedChange = viewModel::setUseTrueBlack,
                    )
                },
            )

            SectionHeader(title = "App icon")
            AppIconSection(
                selected = uiState.appIcon,
                onSelect = viewModel::setAppIcon,
            )
        }
    }

    if (showCustomPicker) {
        var draft by remember { mutableStateOf(uiState.accentSeedArgb) }
        AlertDialog(
            onDismissRequest = { showCustomPicker = false },
            title = { Text("Custom accent color") },
            text = {
                Column {
                    CustomColorPicker(
                        initialColor = Color(uiState.accentSeedArgb),
                        onColorChanged = { draft = it.toArgb() },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setAccentSeed(draft)
                        showCustomPicker = false
                    },
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomPicker = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun AccentColorSection(
    accentArgb: Int,
    onPick: (Int) -> Unit,
    onShowCustom: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = SpaceL)) {
        Text(
            text = "Accent color",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        ColorSwatchPicker(
            selectedColor = Color(accentArgb),
            onColorSelected = { onPick(it.toArgb()) },
        )
        TextButton(onClick = onShowCustom) {
            Text("Custom…")
        }
    }
}

private fun AppIcon.drawableRes(): Int = when (this) {
    AppIcon.DEFAULT -> R.mipmap.ic_launcher
    AppIcon.MIDNIGHT -> R.mipmap.ic_launcher_midnight
    AppIcon.SUNRISE -> R.mipmap.ic_launcher_sunrise
}

private fun AppIcon.label(): String = when (this) {
    AppIcon.DEFAULT -> "Default"
    AppIcon.MIDNIGHT -> "Midnight"
    AppIcon.SUNRISE -> "Sunrise"
}

@Composable
private fun AppIconSection(
    selected: AppIcon,
    onSelect: (AppIcon) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = SpaceL, vertical = SpaceS)) {
        Text(
            text = "Pick a launcher icon",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SpaceM),
        ) {
            AppIcon.entries.forEach { icon ->
                AppIconPreview(
                    icon = icon,
                    selected = icon == selected,
                    onClick = { onSelect(icon) },
                )
            }
        }
    }
}

@Composable
private fun AppIconPreview(
    icon: AppIcon,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    Column(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = icon.drawableRes()),
                contentDescription = icon.label(),
                tint = Color.Unspecified,
                modifier = Modifier.size(56.dp),
            )
        }
        Text(
            text = icon.label(),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
