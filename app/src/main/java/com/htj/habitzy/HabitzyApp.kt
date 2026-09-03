package com.htj.habitzy

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.htj.habitzy.data.local.datastore.AppIcon
import com.htj.habitzy.data.local.datastore.AppPreferences
import com.htj.habitzy.notifications.NotificationScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltAndroidApp
class HabitzyApp : Application(), Configuration.Provider {

    @Inject
    lateinit var notificationScheduler: NotificationScheduler

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var appPreferences: AppPreferences

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        notificationScheduler.createChannel()
        notificationScheduler.rescheduleAll()
        applyPersistedLauncherAlias()
    }

    private fun applyPersistedLauncherAlias() {
        val packageManager = packageManager
        val packageName = packageName
        CoroutineScope(Dispatchers.IO).launch {
            val selected = appPreferences.appIcon.first()
            AppIcon.entries.forEach { icon ->
                val component = ComponentName(packageName, "$packageName.${icon.componentSuffix}")
                val state = if (icon == selected) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                }
                packageManager.setComponentEnabledSetting(
                    component,
                    state,
                    PackageManager.DONT_KILL_APP,
                )
            }
        }
    }
}
