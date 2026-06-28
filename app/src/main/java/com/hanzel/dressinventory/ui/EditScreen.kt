package com.hanzel.dressinventory.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.hanzel.dressinventory.AppViewModel
import com.hanzel.dressinventory.data.BOTTOM_TYPES
import com.hanzel.dressinventory.data.COLOR_CHART
import com.hanzel.dressinventory.data.Category
import com.hanzel.dressinventory.data.Dress
import com.hanzel.dressinventory.data.Pattern
import com.hanzel.dressinventory.data.TOP_TYPES
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditScreen(existing: Dress?, vm: AppViewModel, onClose: () -> Unit) {
    val context = LocalContext.current

    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var category by rememberSaveable { mutableStateOf(existing?.category ?: Category.TOP) }
    var type by rememberSaveable { mutableStateOf(existing?.type ?: "") }
    var colorName by rememberSaveable { mutableStateOf(existing?.colorName ?: "") }
    var colorHex by rememberSaveable { mutableStateOf(existing?.colorHex ?: 0L) }
    var pattern by rememberSaveable { mutableStateOf(existing?.pattern ?: Pattern.SOLID) }
    var photoPath by rememberSaveable { mutableStateOf(existing?.photoPath) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) cameraUri?.let { uri -> vm.importPhoto(uri) { p -> if (p != null) photoPath = p } }
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { vm.importPhoto(it) { p -> if (p != null) photoPath = p } }
    }

    fun launchCamera() {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "shot_${UUID.randomUUID()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        cameraUri = uri
        takePicture.launch(uri)
    }

    val types = if (category == Category.TOP) TOP_TYPES else BOTTOM_TYPES
    val canSave = name.isNotBlank() && type.isNotBlank() && colorName.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "Add a piece" else "Edit piece") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (existing != null) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (existing?.code?.isNotBlank() == true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Wardrobe ID",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 0.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    DressCodeBadge(existing.code)
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                placeholder = { Text("e.g. Sunday floral shirt") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Column {
                SectionLabel("Category")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = category == Category.TOP,
                        onClick = { if (category != Category.TOP) { category = Category.TOP; type = "" } },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    ) { Text("Top wear") }
                    SegmentedButton(
                        selected = category == Category.BOTTOM,
                        onClick = { if (category != Category.BOTTOM) { category = Category.BOTTOM; type = "" } },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    ) { Text("Bottom wear") }
                }
            }

            Column {
                SectionLabel("Type")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    types.forEach { t ->
                        FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t) })
                    }
                }
            }

            Column {
                SectionLabel("Colour — fine-tune in natural light")
                ColorWheelPicker(
                    initialColorHex = if (colorHex == 0L) 0xFFFAFAF7L else colorHex,
                    onColorSelected = { hex, name ->
                        colorHex = hex
                        colorName = name
                    }
                )
            }

            Column {
                SectionLabel("Pattern")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = pattern == Pattern.SOLID,
                        onClick = { pattern = Pattern.SOLID },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    ) { Text("Plain solid") }
                    SegmentedButton(
                        selected = pattern == Pattern.PATTERNED,
                        onClick = { pattern = Pattern.PATTERNED },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    ) { Text("Patterned") }
                }

                if (pattern == Pattern.PATTERNED) {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { launchCamera() }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Outlined.AddAPhoto, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Camera")
                        }
                        OutlinedButton(
                            onClick = {
                                pickImage.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Gallery")
                        }
                    }
                    if (photoPath != null) {
                        Spacer(Modifier.height(12.dp))
                        Box {
                            AsyncImage(
                                model = File(photoPath!!),
                                contentDescription = "Pattern photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(MaterialTheme.shapes.medium),
                            )
                            IconButton(
                                onClick = { photoPath = null },
                                modifier = Modifier.align(Alignment.TopEnd),
                            ) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = "Remove photo",
                                    tint = Color.White,
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    vm.upsert(
                        Dress(
                            id = existing?.id ?: UUID.randomUUID().toString(),
                            code = existing?.code.orEmpty(),
                            name = name.trim(),
                            category = category,
                            type = type,
                            colorName = colorName,
                            colorHex = colorHex,
                            pattern = pattern,
                            photoPath = if (pattern == Pattern.PATTERNED) photoPath else null,
                        )
                    )
                    onClose()
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(if (existing == null) "Add to wardrobe" else "Save changes")
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    if (showDeleteConfirm && existing != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${existing.name}?") },
            text = { Text("This also removes it from your wear log.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(existing.id)
                    showDeleteConfirm = false
                    onClose()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

private fun isLight(hex: Long): Boolean {
    val r = (hex shr 16) and 0xFF
    val g = (hex shr 8) and 0xFF
    val b = hex and 0xFF
    return (0.299 * r + 0.587 * g + 0.114 * b) > 160
}
