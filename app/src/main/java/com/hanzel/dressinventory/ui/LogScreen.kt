package com.hanzel.dressinventory.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.hanzel.dressinventory.data.AppData
import com.hanzel.dressinventory.data.Category
import com.hanzel.dressinventory.data.Dress
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
fun LogScreen(data: AppData, onToggle: (LocalDate, String) -> Unit) {
    val today = remember { LocalDate.now() }
    var selected by remember { mutableStateOf(today) }
    var showCalendar by remember { mutableStateOf(false) }
    val wornIds = data.wearLog[selected.toString()].orEmpty().toSet()

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Wear Log",
            subtitle = selected.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())),
            trailingContent = {
                IconButton(onClick = { showCalendar = true }) {
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        contentDescription = "Log history calendar",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            },
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items((0..29).map { today.minusDays(it.toLong()) }) { date ->
                DayChip(date = date, isToday = date == today, selected = date == selected) {
                    selected = date
                }
            }
        }

        if (data.dresses.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.EventAvailable,
                title = "No dresses to log",
                subtitle = "Add pieces to your wardrobe first, then tap them here to record what you wore each day.",
            )
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val tops = data.dresses.filter { it.category == Category.TOP }
            val bottoms = data.dresses.filter { it.category == Category.BOTTOM }
            if (tops.isNotEmpty()) {
                item {
                    Text(
                        "Top wear",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                    )
                }
                items(tops, key = { "t${it.id}" }) { d ->
                    WearRow(d, worn = d.id in wornIds) { onToggle(selected, d.id) }
                }
            }
            if (bottoms.isNotEmpty()) {
                item {
                    Text(
                        "Bottom wear",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
                    )
                }
                items(bottoms, key = { "b${it.id}" }) { d ->
                    WearRow(d, worn = d.id in wornIds) { onToggle(selected, d.id) }
                }
            }
        }
    }

    if (showCalendar) {
        LogHistoryCalendarDialog(
            wearLog = data.wearLog,
            selected = selected,
            today = today,
            onDateSelected = { date ->
                selected = date
                showCalendar = false
            },
            onDismiss = { showCalendar = false },
        )
    }
}

@Composable
private fun LogHistoryCalendarDialog(
    wearLog: Map<String, List<String>>,
    selected: LocalDate,
    today: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    var displayMonth by remember { mutableStateOf(YearMonth.from(selected)) }
    val loggedDates = remember(wearLog) {
        wearLog.filter { (_, ids) -> ids.isNotEmpty() }.keys.map(LocalDate::parse).toSet()
    }
    val locale = Locale.getDefault()
    val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek
    val weekdayLabels = (0..6).map { offset ->
        firstDayOfWeek.plus(offset.toLong()).getDisplayName(TextStyle.NARROW, locale)
    }
    val monthTitle = displayMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
    val daysInMonth = displayMonth.lengthOfMonth()
    val leadingBlanks = ((displayMonth.atDay(1).dayOfWeek.value - firstDayOfWeek.value + 7) % 7)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { displayMonth = displayMonth.minusMonths(1) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous month",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        monthTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    IconButton(
                        onClick = { displayMonth = displayMonth.plusMonths(1) },
                        enabled = displayMonth.isBefore(YearMonth.from(today)),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next month",
                            tint = if (displayMonth.isBefore(YearMonth.from(today))) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                            },
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    weekdayLabels.forEach { label ->
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                val totalCells = leadingBlanks + daysInMonth
                val rows = (totalCells + 6) / 7
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    var day = 1
                    repeat(rows) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            repeat(7) { col ->
                                val cellIndex = it * 7 + col
                                if (cellIndex < leadingBlanks || day > daysInMonth) {
                                    Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                } else {
                                    val date = displayMonth.atDay(day)
                                    val isFuture = date.isAfter(today)
                                    val isSelected = date == selected
                                    val isToday = date == today
                                    val isLogged = date in loggedDates
                                    CalendarDayCell(
                                        day = day,
                                        isSelected = isSelected,
                                        isToday = isToday,
                                        isLogged = isLogged,
                                        enabled = !isFuture,
                                        onClick = { onDateSelected(date) },
                                        modifier = Modifier.weight(1f),
                                    )
                                    day++
                                }
                            }
                        }
                    }
                }

                Text(
                    "Dots mark days you've logged",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: Int,
    isSelected: Boolean,
    isToday: Boolean,
    isLogged: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        isToday -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.surface
    }
    val textColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(bg)
            .then(
                if (enabled) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$day",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
            )
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(5.dp)
                    .background(
                        if (isLogged) MaterialTheme.colorScheme.primary
                        else Color.Transparent,
                        CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun DayChip(date: LocalDate, isToday: Boolean, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 6.dp)) {
                Text(
                    if (isToday) "Today"
                    else date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())}",
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                )
            }
        },
    )
}

@Composable
private fun WearRow(dress: Dress, worn: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (worn) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            DressVisual(
                dress,
                modifier = Modifier.size(44.dp).clip(MaterialTheme.shapes.small),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
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
                Text(
                    "${dress.colorName} · ${dress.type}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                if (worn) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = if (worn) "Worn" else "Not worn",
                tint = if (worn) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
