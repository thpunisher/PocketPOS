package com.pocketpos.ui.inventory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpos.data.local.PocketPosDatabase
import com.pocketpos.data.local.dev.DevSeeder
import com.pocketpos.data.local.relation.ProductWithDetails
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InventoryViewModel(app: Application) : AndroidViewModel(app) {

    private val db = PocketPosDatabase.getInstance(app)
    private val productDao = db.productDao()

    private val query = MutableStateFlow("")

    /** Re-queries Room every time the text changes AND every time the table changes. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val products: StateFlow<List<ProductWithDetails>> = query
        .flatMapLatest { productDao.observeSearch(it.trim()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalCount: StateFlow<Int> = productDao.observeActiveCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val lowStockCount: StateFlow<Int> = productDao.observeLowStockCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val outOfStockCount: StateFlow<Int> = productDao.observeOutOfStockCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        // First query on the database => Room creates pocketpos.db here (if it doesn't exist yet).
        viewModelScope.launch { DevSeeder.seedIfEmpty(db) }
    }

    fun onQueryChange(text: String) {
        query.value = text
    }
}
