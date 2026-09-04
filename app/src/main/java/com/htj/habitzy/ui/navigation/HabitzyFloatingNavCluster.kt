package com.htj.habitzy.ui.navigation

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.htj.habitzy.R
import com.htj.habitzy.ui.theme.HabitzyElevation
import com.htj.habitzy.ui.theme.HabitzyMotion
import com.htj.habitzy.ui.theme.HabitzyTheme
import com.htj.habitzy.ui.theme.ShapeFull

enum class HabitzyTopLevelTab { Habits, Insights }

/** Bottom content clearance so list items aren't hidden behind the floating cluster. */
val FloatingNavClusterContentClearance = 96.dp

@Composable
fun HabitzyFloatingNavCluster(
    selectedTab: HabitzyTopLevelTab?,
    onSelectHabits: () -> Unit,
    onSelectInsights: () -> Unit,
    onAddHabit: () -> Unit,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(HabitzyMotion.standardSpring) +
            slideInVertically(
                spring<IntOffset>(
                    dampingRatio = 1f,
                    stiffness = 300f,
                ),
            ) { it / 2 },
        exit = fadeOut(HabitzyMotion.standardSpring) +
            slideOutVertically(
                spring<IntOffset>(
                    dampingRatio = 1f,
                    stiffness = 300f,
                ),
            ) { it / 2 },
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                bottom = 16.dp + WindowInsets.navigationBars
                    .asPaddingValues().calculateBottomPadding(),
            ),
        ) {
            HabitzyPillToggleBar(
                selectedTab = selectedTab,
                onSelectHabits = onSelectHabits,
                onSelectInsights = onSelectInsights,
            )
            FloatingActionButton(
                onClick = onAddHabit,
                shape = ShapeFull,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(56.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add habit")
            }
        }
    }
}

@Composable
private fun HabitzyPillToggleBar(
    selectedTab: HabitzyTopLevelTab?,
    onSelectHabits: () -> Unit,
    onSelectInsights: () -> Unit,
) {
    Surface(
        shape = ShapeFull,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = HabitzyElevation.Level2.shadowDp,
    ) {
        Row(
            modifier = Modifier
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            HabitzyToggleItem(
                selected = selectedTab == HabitzyTopLevelTab.Habits,
                icon = Icons.Rounded.Home,
                label = "Habits",
                onClick = onSelectHabits,
            )
            HabitzyToggleItem(
                selected = selectedTab == HabitzyTopLevelTab.Insights,
                iconRes = R.drawable.ic_insights,
                label = "Insights",
                onClick = onSelectInsights,
            )
        }
    }
}

@Composable
private fun HabitzyToggleItem(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    @DrawableRes iconRes: Int? = null,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer
        else Color.Transparent,
        animationSpec = spring<Color>(
            dampingRatio = 0.6f,
            stiffness = 380f,
        ),
        label = "toggleItemBackground",
    )
    val contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clip(ShapeFull)
            .background(backgroundColor)
            .selectable(
                selected = selected,
                role = Role.Tab,
                onClick = onClick,
            )
            .semantics { this.contentDescription = label }
            .padding(horizontal = if (selected) 16.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = contentColor)
        } else if (iconRes != null) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = contentColor,
            )
        }
        AnimatedVisibility(visible = selected) {
            Text(label, color = contentColor, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Preview(name = "Habits selected")
@Composable
private fun ClusterHabitsPreview() {
    HabitzyTheme {
        HabitzyFloatingNavCluster(
            selectedTab = HabitzyTopLevelTab.Habits,
            onSelectHabits = {},
            onSelectInsights = {},
            onAddHabit = {},
            visible = true,
        )
    }
}

@Preview(name = "Insights selected")
@Composable
private fun ClusterInsightsPreview() {
    HabitzyTheme {
        HabitzyFloatingNavCluster(
            selectedTab = HabitzyTopLevelTab.Insights,
            onSelectHabits = {},
            onSelectInsights = {},
            onAddHabit = {},
            visible = true,
        )
    }
}
