package com.htj.habitzy.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.htj.habitzy.ui.navigation.AboutSettingsRoute
import com.htj.habitzy.ui.navigation.AccountSettingsRoute
import com.htj.habitzy.ui.navigation.AddEditHabitRoute
import com.htj.habitzy.ui.navigation.AppearanceSettingsRoute
import com.htj.habitzy.ui.navigation.DataSettingsRoute
import com.htj.habitzy.ui.navigation.HabitDetailRoute
import com.htj.habitzy.ui.navigation.HabitzyNavHost
import com.htj.habitzy.ui.navigation.HabitzyNavigationSuite
import com.htj.habitzy.ui.navigation.HabitsRoute
import com.htj.habitzy.ui.navigation.InsightsRoute
import com.htj.habitzy.ui.navigation.PreferencesSettingsRoute
import com.htj.habitzy.ui.navigation.SettingsRoute

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun HabitzyApp(
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination

    val currentTab: Any? = when {
        destination?.hasRoute<HabitsRoute>() == true ||
            destination?.hasRoute<HabitDetailRoute>() == true ||
            destination?.hasRoute<AddEditHabitRoute>() == true -> HabitsRoute
        destination?.hasRoute<InsightsRoute>() == true -> InsightsRoute
        destination?.hasRoute<SettingsRoute>() == true ||
            destination?.hasRoute<AccountSettingsRoute>() == true ||
            destination?.hasRoute<AppearanceSettingsRoute>() == true ||
            destination?.hasRoute<PreferencesSettingsRoute>() == true ||
            destination?.hasRoute<DataSettingsRoute>() == true ||
            destination?.hasRoute<AboutSettingsRoute>() == true -> SettingsRoute
        else -> null
    }

    SharedTransitionLayout {
        HabitzyNavigationSuite(
            currentDestination = currentTab,
            onNavigateToHabits = { navController.navigateTab(HabitsRoute) },
            onNavigateToInsights = { navController.navigateTab(InsightsRoute) },
            onNavigateToSettings = { navController.navigateTab(SettingsRoute) },
            modifier = modifier,
        ) {
            HabitzyNavHost(navController = navController)
        }
    }
}

private fun NavHostController.navigateTab(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
