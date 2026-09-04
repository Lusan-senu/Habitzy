package com.htj.habitzy.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.htj.habitzy.ui.addedithabit.AddEditHabitScreen
import com.htj.habitzy.ui.habitdetail.HabitDetailScreen
import com.htj.habitzy.ui.habits.HabitsScreen
import com.htj.habitzy.ui.insights.InsightsScreen
import com.htj.habitzy.ui.profile.ProfileScreen
import com.htj.habitzy.ui.settings.SettingsScreen
import com.htj.habitzy.ui.settings.about.AboutSettingsScreen
import com.htj.habitzy.ui.settings.account.AccountSettingsScreen
import com.htj.habitzy.ui.settings.appearance.AppearanceSettingsScreen
import com.htj.habitzy.ui.settings.data.DataSettingsScreen
import com.htj.habitzy.ui.settings.preferences.PreferencesSettingsScreen
import com.htj.habitzy.ui.theme.HabitzyMotion
import com.htj.habitzy.ui.theme.materialSharedAxisXIn
import com.htj.habitzy.ui.theme.materialSharedAxisXOut
import com.htj.habitzy.ui.theme.materialSlideFromEndIn
import com.htj.habitzy.ui.theme.materialSlideToEndOut

@Composable
fun HabitzyNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    // Resolved once per composition here (this function IS @Composable), then captured
    // by the enter/exitTransition lambdas below, which are not.
    val tabSpatialSpec = HabitzyMotion.defaultSpatialSpec<IntOffset>()
    val pushSpatialSpec = HabitzyMotion.fastSpatialSpec<IntOffset>()
    val effectsSpec = HabitzyMotion.defaultEffectsSpec<Float>()

    NavHost(
        navController = navController,
        startDestination = HabitsRoute,
        modifier = modifier,
        enterTransition = { materialSlideFromEndIn(pushSpatialSpec, effectsSpec) },
        exitTransition = { materialSlideToEndOut(pushSpatialSpec, effectsSpec) },
        popEnterTransition = { materialSlideFromEndIn(pushSpatialSpec, effectsSpec) },
        popExitTransition = { materialSlideToEndOut(pushSpatialSpec, effectsSpec) },
    ) {
        composable<HabitsRoute>(
            enterTransition = { materialSharedAxisXIn(tabSpatialSpec, effectsSpec) },
            exitTransition = { materialSharedAxisXOut(tabSpatialSpec, effectsSpec) },
            popEnterTransition = { materialSharedAxisXIn(tabSpatialSpec, effectsSpec) },
            popExitTransition = { materialSharedAxisXOut(tabSpatialSpec, effectsSpec) },
        ) {
            HabitsScreen(
                onNavigateToDetail = { navController.navigate(HabitDetailRoute(it)) },
                onNavigateToAddEdit = { navController.navigate(AddEditHabitRoute(it)) },
                onNavigateToProfile = { navController.navigate(ProfileRoute) },
                onNavigateToSettings = { navController.navigate(SettingsRoute) },
            )
        }

        composable<HabitDetailRoute> { backStackEntry ->
            // unchanged — inherits the NavHost-level push transition
            val route: HabitDetailRoute = backStackEntry.toRoute()
            HabitDetailScreen(
                habitId = route.habitId,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(AddEditHabitRoute(it)) },
            )
        }

        composable<AddEditHabitRoute> { backStackEntry ->
            // unchanged
        }

        composable<InsightsRoute>(
            enterTransition = { materialSharedAxisXIn(tabSpatialSpec, effectsSpec) },
            exitTransition = { materialSharedAxisXOut(tabSpatialSpec, effectsSpec) },
            popEnterTransition = { materialSharedAxisXIn(tabSpatialSpec, effectsSpec) },
            popExitTransition = { materialSharedAxisXOut(tabSpatialSpec, effectsSpec) },
        ) {
            // unchanged body
        }

        composable<SettingsRoute>(
            enterTransition = { materialSharedAxisXIn(tabSpatialSpec, effectsSpec) },
            exitTransition = { materialSharedAxisXOut(tabSpatialSpec, effectsSpec) },
            popEnterTransition = { materialSharedAxisXIn(tabSpatialSpec, effectsSpec) },
            popExitTransition = { materialSharedAxisXOut(tabSpatialSpec, effectsSpec) },
        ) {
            // unchanged body
        }

        // AccountSettingsRoute, AppearanceSettingsRoute, PreferencesSettingsRoute,
        // DataSettingsRoute, AboutSettingsRoute, ProfileRoute: unchanged,
        // still inherit the NavHost-level push transition.
    }
}