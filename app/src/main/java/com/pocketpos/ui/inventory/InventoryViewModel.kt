package com.pocketpos.ui.inventory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpos.data.local.PocketPosDatabase
import com.pocketpos.data.local.dev.DevSeeder
import com.pocketpos.data.local.entity.ProductEntity
import com.pocketpos.data.local.relation.ProductWithDetails
import com.pocketpos.data.local.repository.ProductRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InventoryViewModel(app: Application) : AndroidViewModel(app) {

    private val db = PocketPosDatabase.getInstance(app)
    private val productDao = db.productDao()
    private val productRepo = ProductRepository(db)

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

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        // First query on the database => Room creates pocketpos.db here (if it doesn't exist yet).
        viewModelScope.launch { DevSeeder.seedIfEmpty(db) }
    }

    fun onQueryChange(text: String) {
        query.value = text
    }

    /** TEMPORARY: adds a product so you can watch the list update live. Replace with the real form. */
    fun addTestProduct() {
        viewModelScope.launch {
            val n = System.currentTimeMillis() % 1000
            productRepo
                .create(
                    ProductEntity(name = "Producto de prueba $n", priceCents = 199, stockQuantity = 10),
                    userId = null
                )
                .onSuccess { _messages.tryEmit("Producto añadido") }
                .onFailure { _messages.tryEmit(it.message ?: "Error al guardar") }
        }
    }
}
