package com.hanzel.dressinventory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hanzel.dressinventory.data.Dress
import com.hanzel.dressinventory.ui.ClosetScreen
import com.hanzel.dressinventory.ui.DressTheme
import com.hanzel.dressinventory.ui.EditScreen
import com.hanzel.dressinventory.ui.LogScreen
import com.hanzel.dressinventory.ui.PairsScreen
import com.hanzel.dressinventory.ui.SuggestScreen
import com.hanzel.dressinventory.ui.ShopScreen
import com.hanzel.dressinventory.ui.FullscreenColorView
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DressTheme {
                App()
            }
        }
    }
}

private data class Tab(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val PairListSaver = Saver<List<Pair<Long, String>>, ArrayList<String>>(
    save = { list ->
        list.map { "${it.first}|${it.second}" }.let { ArrayList(it) }
    },
    restore = { arr ->
        arr.map { str ->
            val parts = str.split('|')
            parts[0].toLong() to parts[1]
        }
    }
)

@Composable
private fun App(vm: AppViewModel = viewModel()) {
    val data by vm.data.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var editorOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Dress?>(null) }
    var fullscreenColors by rememberSaveable(stateSaver = PairListSaver) { mutableStateOf(emptyList<Pair<Long, String>>()) }
    var fullscreenInitialIndex by rememberSaveable { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    if (editorOpen) {
        EditScreen(existing = editing, vm = vm, onClose = { editorOpen = false; editing = null })
        return
    }

    if (fullscreenColors.isNotEmpty()) {
        FullscreenColorView(
            colors = fullscreenColors,
            initialIndex = fullscreenInitialIndex,
            onClose = { fullscreenColors = emptyList() }
        )
        return
    }

    val tabs = listOf(
        Tab("Wardrobe", Icons.Outlined.Checkroom, Icons.Filled.Checkroom),
        Tab("Log", Icons.Outlined.EventAvailable, Icons.Filled.EventAvailable),
        Tab("Ideas", Icons.Outlined.AutoAwesome, Icons.Filled.AutoAwesome),
        Tab("Pairs", Icons.Outlined.FavoriteBorder, Icons.Filled.Favorite),
        Tab("Shop", Icons.Outlined.ShoppingBag, Icons.Filled.ShoppingBag),
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(if (tab == i) t.selectedIcon else t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (tab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { editing = null; editorOpen = true },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Add dress") },
                )
            }
        },
    ) { padding ->
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(padding)) {
            when (tab) {
                0 -> ClosetScreen(data = data, onEdit = { editing = it; editorOpen = true })
                1 -> LogScreen(data = data, onToggle = { date, id -> vm.toggleWorn(date, id) })
                2 -> SuggestScreen(
                    data = data,
                    onWear = { date, top, bottom ->
                        vm.wearOutfit(date, top.id, bottom.id)
                        scope.launch {
                            snackbar.showSnackbar(
                                "Outfit logged for ${date.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()))}"
                            )
                        }
                    },
                )
                3 -> PairsScreen(data = data)
                4 -> ShopScreen(
                    data = data,
                    vm = vm,
                    onColorClick = { colors, index ->
                        fullscreenColors = colors
                        fullscreenInitialIndex = index
                    }
                )
            }
        }
    }
}
