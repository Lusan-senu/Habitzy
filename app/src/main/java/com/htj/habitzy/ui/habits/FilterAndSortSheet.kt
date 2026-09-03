package com.htj.habitzy.ui.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.htj.habitzy.ui.theme.ShapeExtraLargeIncreased
import com.htj.habitzy.ui.theme.SpaceL
import com.htj.habitzy.ui.theme.SpaceM
import com.htj.habitzy.ui.theme.SpaceS
import com.htj.habitzy.ui.theme.SpaceXL

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterAndSortSheet(
    showArchived: Boolean,
    onToggleShowArchived: (Boolean) -> Unit,
    availableTags: List<String>,
    activeFilters: Set<String>,
    onToggleFilter: (String) -> Unit,
    sortMode: SortMode,
    onSelectSortMode: (SortMode) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = ShapeExtraLargeIncreased,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = SpaceXL),
        ) {
            Text(
                text = "Filter & sort",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = SpaceL, vertical = SpaceS),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SpaceL, vertical = SpaceS),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Show archived habits", style = MaterialTheme.typography.bodyLarge)
                }
                Switch(checked = showArchived, onCheckedChange = onToggleShowArchived)
            }

            if (availableTags.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = SpaceS))
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = SpaceL, vertical = SpaceS),
                )
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SpaceL),
                    horizontalArrangement = Arrangement.spacedBy(SpaceS),
                    verticalArrangement = Arrangement.spacedBy(SpaceS),
                ) {
                    availableTags.forEach { tag ->
                        FilterChip(
                            selected = tag in activeFilters,
                            onClick = { onToggleFilter(tag) },
                            label = { Text(text = tag) },
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = SpaceS))
            Text(
                text = "Sort by",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = SpaceL, vertical = SpaceS),
            )
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SpaceL, vertical = SpaceS),
            ) {
                val options = listOf(
                    SortMode.MANUAL to "Manual",
                    SortMode.NAME to "Name",
                    SortMode.CREATED_DATE to "Created",
                    SortMode.CURRENT_STREAK to "Streak",
                )
                options.forEachIndexed { index, (mode, label) ->
                    SegmentedButton(
                        selected = sortMode == mode,
                        onClick = { onSelectSortMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    ) {
                        Text(text = label, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
