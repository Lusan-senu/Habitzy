package com.htj.habitzy.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.htj.habitzy.ui.theme.HabitzyTheme
import com.htj.habitzy.ui.theme.ShapeExtraLargeIncreased
import com.htj.habitzy.ui.theme.ShapeFull

@Composable
fun HabitzyTopActionPill(
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    filterSortAvailable: Boolean = false,
    onFilterSortClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier,
        shape = ShapeFull,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier
                .height(40.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onProfileClick,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = "Profile",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More options",
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    shape = ShapeExtraLargeIncreased,
                ) {
                    if (filterSortAvailable) {
                        DropdownMenuItem(
                            text = { Text("Filter & sort") },
                            onClick = {
                                menuExpanded = false
                                onFilterSortClick()
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Settings") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Settings, contentDescription = null)
                        },
                        onClick = {
                            menuExpanded = false
                            onSettingsClick()
                        },
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Preview(name = "Pill — light")
@Composable
private fun HabitzyTopActionPillPreviewLight() {
    HabitzyTheme {
        HabitzyTopActionPill(
            onProfileClick = {},
            onSettingsClick = {},
            filterSortAvailable = true,
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Preview(name = "Pill — dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HabitzyTopActionPillPreviewDark() {
    HabitzyTheme {
        HabitzyTopActionPill(
            onProfileClick = {},
            onSettingsClick = {},
            filterSortAvailable = false,
        )
    }
}
