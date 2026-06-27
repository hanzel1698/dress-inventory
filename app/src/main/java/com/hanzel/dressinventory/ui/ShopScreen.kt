package com.hanzel.dressinventory.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hanzel.dressinventory.AppViewModel
import com.hanzel.dressinventory.data.AppData
import com.hanzel.dressinventory.data.ColorRecommendation
import com.hanzel.dressinventory.data.getShoppingRecommendations
import com.hanzel.dressinventory.data.matchLabel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShopScreen(
    data: AppData,
    vm: AppViewModel,
    onColorClick: (List<Pair<Long, String>>, Int) -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var filterIndex by rememberSaveable { mutableIntStateOf(0) } // 0: All, 1: Wishlist, 2: Neutrals, 3: Accents

    val allRecs = remember(data) { getShoppingRecommendations(data) }
    
    val filteredRecs = remember(allRecs, searchQuery, filterIndex, data.shoppingWishlist) {
        allRecs.filter { rec ->
            // Search filter
            val matchesSearch = rec.color.name.contains(searchQuery, ignoreCase = true) ||
                    (rec.topMatchDress?.name?.contains(searchQuery, ignoreCase = true) ?: false) ||
                    (rec.bottomMatchDress?.name?.contains(searchQuery, ignoreCase = true) ?: false)
            
            // Tab/category filter
            val matchesFilter = when (filterIndex) {
                1 -> rec.color.name in data.shoppingWishlist
                2 -> rec.color.neutral
                3 -> !rec.color.neutral
                else -> true
            }
            
            matchesSearch && matchesFilter
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Shop Inspiration",
            subtitle = if (allRecs.isEmpty()) "Your wardrobe is complete!" 
            else "${allRecs.size} nice colours missing from your closet"
        )

        // Search text field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search colours or matching pieces...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )

        // Filter chips row
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Wishlist", "Neutrals", "Accents").forEachIndexed { i, label ->
                FilterChip(
                    selected = filterIndex == i,
                    onClick = { filterIndex = i },
                    label = {
                        Text(
                            text = if (i == 1) "$label (${data.shoppingWishlist.size})" else label
                        )
                    }
                )
            }
        }

        if (filteredRecs.isEmpty()) {
            val emptyTitle = if (searchQuery.isNotEmpty()) "No matches found" else "No suggestions here"
            val emptySubtitle = if (searchQuery.isNotEmpty()) "Try searching for a different colour name or item."
            else when (filterIndex) {
                1 -> "Tap the heart icon on any recommended color to save it to your wishlist!"
                else -> "All colours from our chart are already represented in your wardrobe!"
            }
            
            EmptyState(
                icon = Icons.Outlined.ShoppingBag,
                title = emptyTitle,
                subtitle = emptySubtitle,
                modifier = Modifier.weight(1f)
            )
        } else {
            val colorsList = remember(filteredRecs) {
                filteredRecs.map { it.color.hex to it.color.name }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(filteredRecs, key = { _, rec -> rec.color.name }) { index, rec ->
                    val isWishlisted = rec.color.name in data.shoppingWishlist
                    ShoppingCard(
                        rec = rec,
                        isWishlisted = isWishlisted,
                        onWishlistToggle = { vm.toggleWishlistColor(rec.color.name) },
                        onColorClick = { onColorClick(colorsList, index) }
                    )
                }
            }
        }
    }
}

@Composable
fun ShoppingCard(
    rec: ColorRecommendation,
    isWishlisted: Boolean,
    onWishlistToggle: () -> Unit,
    onColorClick: () -> Unit,
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Color Swatch + Name + Heart
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onColorClick() }
                ) {
                    // Curved Swatch with Vertical Gradient (lerp lighting rule)
                    val baseColor = Color(rec.color.hex)
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        lerp(baseColor, Color.White, 0.15f),
                                        baseColor,
                                        lerp(baseColor, Color.Black, 0.18f)
                                    )
                                )
                            )
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = rec.color.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (rec.color.neutral) "Versatile Neutral" else "Vibrant Accent",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }

                // Wishlist Button
                IconButton(onClick = onWishlistToggle) {
                    Icon(
                        imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Toggle Wishlist",
                        tint = if (isWishlisted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Sub-cards showing Top and Bottom matching configurations
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Suggestion Block
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "As a Top wear",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    if (rec.topMatchScore != null && rec.topMatchDress != null) {
                        MatchChip(score = rec.topMatchScore, label = matchLabel(rec.topMatchScore))
                        Spacer(Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DressVisual(
                                dress = rec.topMatchDress,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(MaterialTheme.shapes.small)
                            )
                            Column {
                                Text(
                                    text = rec.topMatchDress.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = rec.topMatchDress.type,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Add bottom wear to match.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }

                // Bottom Suggestion Block
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "As a Bottom wear",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    if (rec.bottomMatchScore != null && rec.bottomMatchDress != null) {
                        MatchChip(score = rec.bottomMatchScore, label = matchLabel(rec.bottomMatchScore))
                        Spacer(Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DressVisual(
                                dress = rec.bottomMatchDress,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(MaterialTheme.shapes.small)
                            )
                            Column {
                                Text(
                                    text = rec.bottomMatchDress.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = rec.bottomMatchDress.type,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Add top wear to match.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}
