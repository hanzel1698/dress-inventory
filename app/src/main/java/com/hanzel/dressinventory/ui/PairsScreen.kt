package com.hanzel.dressinventory.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hanzel.dressinventory.data.AppData
import com.hanzel.dressinventory.data.allPairs
import com.hanzel.dressinventory.data.matchLabel
import com.hanzel.dressinventory.data.matchReason

@Composable
fun PairsScreen(data: AppData) {
    val pairs = remember(data) { allPairs(data).filter { it.matchScore >= 58 }.take(40) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Great Pairs",
            subtitle = "Top + bottom combos ranked by colour & pattern harmony",
        )

        if (pairs.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.FavoriteBorder,
                title = "No pairs yet",
                subtitle = "Add tops and bottoms with their colours and I'll show you which ones belong together.",
            )
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(pairs, key = { "${it.top.id}_${it.bottom.id}" }) { p ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DressVisual(
                                p.top,
                                modifier = Modifier.size(56.dp).clip(MaterialTheme.shapes.small),
                            )
                            Spacer(Modifier.width(8.dp))
                            DressVisual(
                                p.bottom,
                                modifier = Modifier.size(56.dp).clip(MaterialTheme.shapes.small),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${p.top.name} + ${p.bottom.name}",
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.height(4.dp))
                                MatchChip(score = p.matchScore, label = matchLabel(p.matchScore))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            matchReason(p.top, p.bottom),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
