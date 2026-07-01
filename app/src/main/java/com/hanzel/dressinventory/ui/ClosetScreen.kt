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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hanzel.dressinventory.data.AppData
import com.hanzel.dressinventory.data.Category
import com.hanzel.dressinventory.data.Dress

@Composable
fun ClosetScreen(data: AppData, onEdit: (Dress) -> Unit) {
    var filter by rememberSaveable { mutableIntStateOf(0) } // 0 all, 1 tops, 2 bottoms
    val tops = data.dresses.count { it.category == Category.TOP }
    val bottoms = data.dresses.count { it.category == Category.BOTTOM }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "My Wardrobe",
            subtitle = if (data.dresses.isEmpty()) "Your wardrobe, organised"
            else "${data.dresses.size} pieces · $tops tops · $bottoms bottoms",
        )

        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("All", "Top wear", "Bottom wear").forEachIndexed { i, label ->
                FilterChip(selected = filter == i, onClick = { filter = i }, label = { Text(label) })
            }
        }

        val shown = when (filter) {
            1 -> data.dresses.filter { it.category == Category.TOP }
            2 -> data.dresses.filter { it.category == Category.BOTTOM }
            else -> data.dresses
        }

        if (shown.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.Checkroom,
                title = "Nothing here yet",
                subtitle = "Tap the + button to add your first piece — give it a name, pick its colour and type.",
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(shown, key = { it.id }) { dress ->
                    DressCard(dress = dress, onClick = { onEdit(dress) })
                }
            }
        }
    }
}

@Composable
private fun DressCard(dress: Dress, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick) {
        DressVisual(dress, modifier = Modifier.fillMaxWidth().height(150.dp))
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (dress.code.isNotBlank()) {
                    DressCodeBadge(dress.code)
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    dress.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ColorDot(dress.colorHex)
                Spacer(Modifier.width(6.dp))
                Text(
                    "${dress.colorName} · ${dress.type}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
