package com.htj.habitzy.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object HabitsRoute

@Serializable
data class HabitDetailRoute(val habitId: Long)

@Serializable
data class AddEditHabitRoute(val habitId: Long? = null)

@Serializable
object InsightsRoute

@Serializable
object SettingsRoute

@Serializable
object AccountSettingsRoute

@Serializable
object AppearanceSettingsRoute

@Serializable
object PreferencesSettingsRoute

@Serializable
object DataSettingsRoute

@Serializable
object AboutSettingsRoute

@Serializable
object ProfileRoute
