package com.hanzel.dressinventory.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hanzel.dressinventory.data.AppData
import com.hanzel.dressinventory.data.Dress
import com.hanzel.dressinventory.data.freshnessText
import com.hanzel.dressinventory.data.matchLabel
import com.hanzel.dressinventory.data.matchReason
import com.hanzel.dressinventory.data.suggestOutfits
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SuggestScreen(
    data: AppData,
    onWear: (LocalDate, Dress, Dress) -> Unit,
) {
    var dayOffset by remember { mutableIntStateOf(1) } // 0 today, 1 tomorrow
    val date = LocalDate.now().plusDays(dayOffset.toLong())
    val suggestions = remember(data, date) { suggestOutfits(data, date) }
    var index by remember(data, date) { mutableIntStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(
            title = "Outfit Ideas",
            subtitle = "Fresh picks that skip recently worn pieces",
        )

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        ) {
            listOf("Today", "Tomorrow").forEachIndexed { i, label ->
                SegmentedButton(
                    selected = dayOffset == i,
                    onClick = { dayOffset = i },
                    shape = SegmentedButtonDefaults.itemShape(index = i, count = 2),
                ) { Text(label) }
            }
        }

        if (suggestions.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.AutoAwesome,
                title = "No ideas yet",
                subtitle = "Add at least one top and one bottom to your wardrobe and I'll start styling you.",
            )
            return
        }

        val s = suggestions[index % suggestions.size]

        ElevatedCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp).fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutfitHalf(s.top, freshnessText(data, s.top.id, date), Modifier.weight(1f))
                    OutfitHalf(s.bottom, freshnessText(data, s.bottom.id, date), Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
                MatchChip(score = s.matchScore, label = matchLabel(s.matchScore))
                Spacer(Modifier.height(8.dp))
                Text(
                    matchReason(s.top, s.bottom),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row(
            modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = { index++ }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Another idea")
            }
            Button(
                onClick = { onWear(date, s.top, s.bottom) },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Wear this")
            }
        }

        Text(
            "Idea ${index % suggestions.size + 1} of ${suggestions.size}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun OutfitHalf(dress: Dress, freshness: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        DressVisual(
            dress,
            modifier = Modifier.fillMaxWidth().height(150.dp).clip(MaterialTheme.shapes.medium),
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (dress.code.isNotBlank()) {
                DressCodeBadge(dress.code)
                Spacer(Modifier.width(6.dp))
            }
            Text(
                dress.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            ColorDot(dress.colorHex, size = 10)
            Spacer(Modifier.width(4.dp))
            Text(
                "${dress.colorName} ${dress.type}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            freshness,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
        )
    }
}
