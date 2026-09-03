package com.htj.habitzy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.htj.habitzy.ui.theme.ShapeFull
import com.htj.habitzy.ui.theme.SpaceS

private val CuratedEmojis = listOf(
    "\uD83C\uDFC3", "\uD83D\uDCA7", "\uD83D\uDC83", "\uD83E\uDDD8",
    "\uD83D\uDCD6", "\uD83E\uDDD0", "\uD83C\uDFB5", "\uD83C\uDFA8",
    "\uD83D\uDCB0", "\uD83E\uDDF0", "\uD83C\uDF3E", "\u2B50",
    "\uD83D\uDCAA", "\uD83C\uDFCA", "\uD83E\uDDD2", "\uD83C\uDFB8",
    "\uD83D\uDCDA", "\uD83E\uDDE0", "\uD83C\uDF1F", "\uD83D\uDE4C",
    "\uD83C\uDF39", "\uD83D\uDCA1", "\uD83C\uDF08", "\uD83C\uDF3B",
    "\uD83D\uDC7B", "\uD83C\uDF52", "\uD83C\uDF53", "\uD83C\uDF54",
    "\uD83C\uDF5D", "\uD83C\uDF5E", "\uD83C\uDF63", "\uD83C\uDF70",
    "\uD83C\uDF7C", "\uD83C\uDF76", "\u2615", "\uD83C\uDF43",
    "\uD83C\uDF35", "\uD83D\uDC8D", "\uD83C\uDFC6", "\uD83C\uDFC9",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconOrEmojiPicker(
    selectedEmoji: String?,
    onEmojiSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(SpaceS),
        verticalArrangement = Arrangement.spacedBy(SpaceS),
    ) {
        CuratedEmojis.forEach { emoji ->
            val isSelected = emoji == selectedEmoji
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(ShapeFull)
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        }
                    )
                    .clickable { onEmojiSelected(emoji) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}
