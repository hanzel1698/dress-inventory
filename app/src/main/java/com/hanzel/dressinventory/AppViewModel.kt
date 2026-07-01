package com.hanzel.dressinventory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hanzel.dressinventory.data.AppData
import com.hanzel.dressinventory.data.Dress
import com.hanzel.dressinventory.data.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository(app)
    val data: StateFlow<AppData> = repo.data

    fun upsert(dress: Dress) = viewModelScope.launch(Dispatchers.IO) { repo.upsert(dress) }

    fun delete(id: String) = viewModelScope.launch(Dispatchers.IO) { repo.delete(id) }

    fun toggleWorn(date: LocalDate, id: String) =
        viewModelScope.launch(Dispatchers.IO) { repo.toggleWorn(date.toString(), id) }

    fun wearOutfit(date: LocalDate, topId: String, bottomId: String) =
        viewModelScope.launch(Dispatchers.IO) { repo.markWorn(date.toString(), listOf(topId, bottomId)) }

    fun toggleWishlistColor(colorName: String) =
        viewModelScope.launch(Dispatchers.IO) { repo.toggleWishlistColor(colorName) }

}
