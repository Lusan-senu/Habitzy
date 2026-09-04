package com.htj.habitzy.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.htj.habitzy.ui.navigation.AddEditHabitRoute
import com.htj.habitzy.ui.navigation.HabitzyFloatingNavCluster
import com.htj.habitzy.ui.navigation.HabitzyNavHost
import com.htj.habitzy.ui.navigation.HabitzyTopLevelTab
import com.htj.habitzy.ui.navigation.HabitsRoute
import com.htj.habitzy.ui.navigation.InsightsRoute

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun HabitzyApp(
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination

    val selectedTab = when {
        destination?.hasRoute<HabitsRoute>() == true -> HabitzyTopLevelTab.Habits
        destination?.hasRoute<InsightsRoute>() == true -> HabitzyTopLevelTab.Insights
        else -> null
    }
    val showCluster = selectedTab != null

    SharedTransitionLayout {
        Box(modifier = modifier.fillMaxSize()) {
            HabitzyNavHost(navController = navController)

            HabitzyFloatingNavCluster(
                selectedTab = selectedTab,
                onSelectHabits = { navController.navigateTab(HabitsRoute) },
                onSelectInsights = { navController.navigateTab(InsightsRoute) },
                onAddHabit = { navController.navigate(AddEditHabitRoute(habitId = null)) },
                visible = showCluster,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding(),
            )
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
