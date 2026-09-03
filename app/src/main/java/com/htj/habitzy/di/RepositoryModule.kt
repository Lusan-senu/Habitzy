package com.htj.habitzy.di

import com.htj.habitzy.data.repository.BackupRepositoryImpl
import com.htj.habitzy.data.repository.HabitRepositoryImpl
import com.htj.habitzy.data.repository.InsightsRepositoryImpl
import com.htj.habitzy.domain.repository.BackupRepository
import com.htj.habitzy.domain.repository.HabitRepository
import com.htj.habitzy.domain.repository.InsightsRepository
import com.htj.habitzy.domain.repository.ProfileRepository
import com.htj.habitzy.domain.repository.SettingsRepository
import com.htj.habitzy.domain.repository.SettingsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindHabitRepository(impl: HabitRepositoryImpl): HabitRepository

    @Binds
    @Singleton
    abstract fun bindInsightsRepository(impl: InsightsRepositoryImpl): InsightsRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: SettingsRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository
}
