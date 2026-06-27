package com.hanzel.dressinventory.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class Repository(private val context: Context) {

    private val file = File(context.filesDir, "closet.json")
    private val photosDir = File(context.filesDir, "photos").apply { mkdirs() }
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _data = MutableStateFlow(load())
    val data: StateFlow<AppData> = _data.asStateFlow()

    private fun load(): AppData =
        runCatching { json.decodeFromString<AppData>(file.readText()) }.getOrDefault(AppData())

    @Synchronized
    private fun persist(d: AppData) {
        _data.value = d
        runCatching { file.writeText(json.encodeToString(AppData.serializer(), d)) }
    }

    fun upsert(dress: Dress) {
        val cur = _data.value
        val old = cur.dresses.find { it.id == dress.id }
        if (old?.photoPath != null && old.photoPath != dress.photoPath) {
            runCatching { File(old.photoPath).delete() }
        }
        val idx = cur.dresses.indexOfFirst { it.id == dress.id }
        val list = if (idx >= 0) cur.dresses.toMutableList().apply { set(idx, dress) }
        else cur.dresses + dress
        persist(cur.copy(dresses = list))
    }

    fun delete(id: String) {
        val cur = _data.value
        cur.dresses.find { it.id == id }?.photoPath?.let { runCatching { File(it).delete() } }
        val log = cur.wearLog
            .mapValues { (_, ids) -> ids.filterNot { it == id } }
            .filterValues { it.isNotEmpty() }
        persist(cur.copy(dresses = cur.dresses.filterNot { it.id == id }, wearLog = log))
    }

    fun toggleWorn(date: String, id: String) {
        val cur = _data.value
        val ids = cur.wearLog[date].orEmpty()
        val newIds = if (id in ids) ids - id else ids + id
        val log = cur.wearLog.toMutableMap()
        if (newIds.isEmpty()) log.remove(date) else log[date] = newIds
        persist(cur.copy(wearLog = log))
    }

    fun markWorn(date: String, idsToAdd: List<String>) {
        val cur = _data.value
        val ids = (cur.wearLog[date].orEmpty() + idsToAdd).distinct()
        persist(cur.copy(wearLog = cur.wearLog + (date to ids)))
    }

    fun toggleWishlistColor(colorName: String) {
        val cur = _data.value
        val list = cur.shoppingWishlist
        val newList = if (colorName in list) list - colorName else list + colorName
        persist(cur.copy(shoppingWishlist = newList))
    }

    /** Copies and downscales an image into app storage; returns the saved file path. */
    fun importPhoto(uri: Uri): String? = runCatching {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val maxDim = maxOf(bounds.outWidth, bounds.outHeight)
        if (maxDim <= 0) return null
        var sample = 1
        while (maxDim / (sample * 2) >= 1200) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: return null
        val out = File(photosDir, "${UUID.randomUUID()}.jpg")
        out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        bitmap.recycle()
        out.absolutePath
    }.getOrNull()
}
