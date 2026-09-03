package com.htj.habitzy.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.ExperimentalMaterial3AdaptiveNavigationSuiteApi
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.htj.habitzy.R

@OptIn(ExperimentalMaterial3AdaptiveNavigationSuiteApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HabitzyNavigationSuite(
    currentDestination: Any?,
    onNavigateToHabits: () -> Unit,
    onNavigateToInsights: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    NavigationSuiteScaffold(
        modifier = modifier,
        navigationSuiteItems = {
            item(
                selected = currentDestination == HabitsRoute,
                onClick = onNavigateToHabits,
                icon = { Icon(imageVector = Icons.Rounded.Home, contentDescription = "Habits") },
                label = { Text("Habits") },
            )
            item(
                selected = currentDestination == InsightsRoute,
                onClick = onNavigateToInsights,
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_insights),
                        contentDescription = "Insights",
                    )
                },
                label = { Text("Insights") },
            )
            item(
                selected = currentDestination == SettingsRoute,
                onClick = onNavigateToSettings,
                icon = { Icon(imageVector = Icons.Rounded.Settings, contentDescription = "Settings") },
                label = { Text("Settings") },
            )
        },
    ) {
        content()
    }
}
