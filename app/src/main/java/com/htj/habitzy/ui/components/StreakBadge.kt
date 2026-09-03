package com.htj.habitzy.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.SpringSpec
import com.htj.habitzy.R
import com.htj.habitzy.ui.theme.HabitzyMotion
import com.htj.habitzy.ui.theme.ShapeFull
import com.htj.habitzy.ui.theme.SpaceS
import com.htj.habitzy.ui.theme.SpaceXS

@Composable
fun StreakBadge(
    streak: Int,
    modifier: Modifier = Modifier,
) {
    var prevStreak by remember { mutableIntStateOf(streak) }
    var bounce by remember { mutableIntStateOf(0) }

    LaunchedEffect(streak) {
        if (streak > prevStreak) {
            bounce++
        }
        prevStreak = streak
    }

    val scale by animateFloatAsState(
        targetValue = if (bounce > 0) 1.2f else 1f,
        animationSpec = HabitzyMotion.celebrationSpring as SpringSpec<Float>,
        finishedListener = { /* reset handled by next LaunchedEffect */ },
        label = "streak_bounce",
    )

    Row(
        modifier = modifier
            .scale(scale)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = ShapeFull,
            )
            .padding(horizontal = SpaceS, vertical = SpaceXS),
        horizontalArrangement = Arrangement.spacedBy(SpaceXS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_streak),
            contentDescription = "Streak",
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "$streak",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
