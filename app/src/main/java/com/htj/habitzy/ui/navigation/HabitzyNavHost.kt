package com.htj.habitzy.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import com.htj.habitzy.ui.theme.sharedAxisXIn
import com.htj.habitzy.ui.theme.sharedAxisXOut
import com.htj.habitzy.ui.theme.slideFromEndIn
import com.htj.habitzy.ui.theme.slideToEndOut

@Composable
fun HabitzyNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = HabitsRoute,
        modifier = modifier,
        enterTransition = { slideFromEndIn() },
        exitTransition = { slideToEndOut() },
        popEnterTransition = { slideFromEndIn() },
        popExitTransition = { slideToEndOut() },
    ) {
        composable<HabitsRoute>(
            enterTransition = { sharedAxisXIn() },
            exitTransition = { sharedAxisXOut() },
            popEnterTransition = { sharedAxisXIn() },
            popExitTransition = { sharedAxisXOut() },
        ) {
            HabitsScreen(
                onNavigateToDetail = { navController.navigate(HabitDetailRoute(it)) },
                onNavigateToAddEdit = { navController.navigate(AddEditHabitRoute(it)) },
                onNavigateToProfile = { navController.navigate(ProfileRoute) },
            )
        }

        composable<HabitDetailRoute> { backStackEntry ->
            val route: HabitDetailRoute = backStackEntry.toRoute()
            HabitDetailScreen(
                habitId = route.habitId,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(AddEditHabitRoute(it)) },
            )
        }

        composable<AddEditHabitRoute> { backStackEntry ->
            val route: AddEditHabitRoute = backStackEntry.toRoute()
            AddEditHabitScreen(
                habitId = route.habitId,
                onDone = { navController.popBackStack() },
                onDismiss = { navController.popBackStack() },
            )
        }

        composable<InsightsRoute>(
            enterTransition = { sharedAxisXIn() },
            exitTransition = { sharedAxisXOut() },
            popEnterTransition = { sharedAxisXIn() },
            popExitTransition = { sharedAxisXOut() },
        ) {
            InsightsScreen()
        }

        composable<SettingsRoute>(
            enterTransition = { sharedAxisXIn() },
            exitTransition = { sharedAxisXOut() },
            popEnterTransition = { sharedAxisXIn() },
            popExitTransition = { sharedAxisXOut() },
        ) {
            SettingsScreen(
                onAccountClick = { navController.navigate(AccountSettingsRoute) },
                onAppearanceClick = { navController.navigate(AppearanceSettingsRoute) },
                onPreferencesClick = { navController.navigate(PreferencesSettingsRoute) },
                onDataClick = { navController.navigate(DataSettingsRoute) },
                onAboutClick = { navController.navigate(AboutSettingsRoute) },
            )
        }

        composable<AccountSettingsRoute> {
            AccountSettingsScreen(
                onBack = { navController.popBackStack() },
                onProfileClick = { navController.navigate(ProfileRoute) },
            )
        }
        composable<AppearanceSettingsRoute> {
            AppearanceSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable<PreferencesSettingsRoute> {
            PreferencesSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable<DataSettingsRoute> {
            DataSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable<AboutSettingsRoute> {
            AboutSettingsScreen(onBack = { navController.popBackStack() })
        }

        composable<ProfileRoute> {
            ProfileScreen(onBack = { navController.popBackStack() })
        }
    }
}
