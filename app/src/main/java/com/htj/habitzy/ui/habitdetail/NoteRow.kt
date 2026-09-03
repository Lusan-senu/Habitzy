package com.htj.habitzy.ui.habitdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.htj.habitzy.domain.model.DayNote
import com.htj.habitzy.ui.theme.HabitzyShapes
import com.htj.habitzy.ui.theme.SpaceM
import com.htj.habitzy.ui.theme.SpaceS
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun NoteRow(
    date: java.time.LocalDate,
    text: String,
    photoUri: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = HabitzyShapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(SpaceM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (photoUri != null) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = null,
                    modifier = Modifier
                        .width(56.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = if (photoUri != null) SpaceS else 0.dp),
            ) {
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (text.isNotBlank()) {
                    Text(text = text, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
