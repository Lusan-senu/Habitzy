package com.htj.habitzy.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.htj.habitzy.ui.components.SectionHeader
import com.htj.habitzy.ui.theme.SpaceL
import com.htj.habitzy.ui.theme.SpaceS
import com.htj.habitzy.ui.theme.SpaceXS

private data class SettingsItem(
    val title: String,
    val subtitle: String?,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

private data class SettingsGroup(
    val title: String,
    val items: List<SettingsItem>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onAccountClick: () -> Unit,
    onAppearanceClick: () -> Unit,
    onPreferencesClick: () -> Unit,
    onDataClick: () -> Unit,
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
            )
        },
    ) { innerPadding ->
        val groups = listOf(
            SettingsGroup(
                title = "General",
                items = listOf(
                    SettingsItem("Appearance", "Theme, colors, and background", Icons.Filled.Build, onAppearanceClick),
                    SettingsItem("Preferences", "Week start, reminders, haptics", Icons.Filled.List, onPreferencesClick),
                ),
            ),
            SettingsGroup(
                title = "Data & account",
                items = listOf(
                    SettingsItem("Account", "Local profile and app lock", Icons.Filled.Person, onAccountClick),
                    SettingsItem("Data", "Backup, restore, and import", Icons.Filled.Star, onDataClick),
                    SettingsItem("About", "Version and licenses", Icons.Filled.Info, onAboutClick),
                ),
            ),
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            groups.forEach { group ->
                item(key = "header_${group.title}") {
                    SectionHeader(title = group.title)
                }
                items(group.items, key = { group.title + it.title }) { item ->
                    ListItem(
                        headlineContent = {
                            Text(item.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        supportingContent = item.subtitle?.let {
                            {
                                Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        },
                        leadingContent = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        modifier = Modifier.clickable(onClick = item.onClick),
                    )
                }
            }
        }
    }
}
