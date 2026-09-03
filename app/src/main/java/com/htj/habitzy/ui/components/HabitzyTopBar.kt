package com.htj.habitzy.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.htj.habitzy.ui.theme.SpaceL
import com.htj.habitzy.ui.theme.SpaceS

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitzyTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    large: Boolean = false,
) {
    if (large) {
        LargeTopAppBar(
            title = {
                Column {
                    Text(text = title, style = MaterialTheme.typography.headlineMedium)
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            modifier = modifier,
            navigationIcon = navigationIcon ?: {},
            actions = actions,
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.largeTopAppBarColors(),
        )
    } else {
        CenterAlignedTopAppBar(
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = title, style = MaterialTheme.typography.titleLarge)
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            modifier = modifier,
            navigationIcon = navigationIcon ?: {},
            actions = actions,
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
        )
    }
}
