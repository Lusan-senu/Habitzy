package com.htj.habitzy.domain.repository

import com.htj.habitzy.domain.model.InsightSummary
import com.htj.habitzy.domain.model.StreakInfo
import com.htj.habitzy.domain.usecase.InsightPeriod
import kotlinx.coroutines.flow.Flow

interface InsightsRepository {
    fun observeStreakInfo(habitId: Long): Flow<StreakInfo>
    fun observeGlobalInsights(period: InsightPeriod): Flow<InsightSummary>
}
