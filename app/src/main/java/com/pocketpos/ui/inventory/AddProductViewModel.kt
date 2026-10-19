package com.pocketpos.ui.inventory

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpos.data.local.PocketPosDatabase
import com.pocketpos.data.local.entity.CategoryEntity
import com.pocketpos.data.local.entity.ProductEntity
import com.pocketpos.data.local.entity.TaxRateEntity
import com.pocketpos.data.local.repository.ProductRepository
import java.math.RoundingMode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Everything the user types, kept as text until "Guardar" is pressed. */
data class ProductForm(
    val name: String = "",
    val categoryId: Long? = null,
    /** null = use the default tax rate */
    val taxRateId: Long? = null,
    val price: String = "",
    val cost: String = "",
    val stock: String = "",
    val lowStock: String = "5",
    val barcode: String = "",
    val sku: String = "",
    val trackStock: Boolean = true
)

data class ProductFormErrors(
    val name: String? = null,
    val price: String? = null,
    val cost: String? = null,
    val stock: String? = null,
    val lowStock: String? = null
) {
    val hasAny: Boolean
        get() = listOf(name, price, cost, stock, lowStock).any { it != null }
}

class AddProductViewModel(app: Application) : AndroidViewModel(app) {

    private val db = PocketPosDatabase.getInstance(app)
    private val repo = ProductRepository(db)

    // Compose state (not StateFlow) so typing in the text fields never lags or jumps the cursor.
    var form by mutableStateOf(ProductForm())
        private set
    var errors by mutableStateOf(ProductFormErrors())
        private set
    var saving by mutableStateOf(false)
        private set

    val categories: StateFlow<List<CategoryEntity>> = db.categoryDao().observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val taxRates: StateFlow<List<TaxRateEntity>> = db.taxRateDao().observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun onFormChange(newForm: ProductForm) {
        form = newForm
    }

    /** Clears the form (the ViewModel lives as long as the Activity, so we reset it by hand). */
    fun reset() {
        form = ProductForm()
        errors = ProductFormErrors()
        saving = false
    }

    /** Creates a category on the fly and selects it. */
    fun createCategory(rawName: String) {
        val name = rawName.trim()
        if (name.isEmpty()) return
        viewModelScope.launch {
            val existing = db.categoryDao().findByName(name)
            val id = existing?.id ?: db.categoryDao().insert(CategoryEntity(name = name))
            form = form.copy(categoryId = id)
        }
    }

    fun save(onSaved: () -> Unit) {
        if (saving) return
        val f = form

        val price = parseMoney(f.price)
        val cost = if (f.cost.isBlank()) 0L else parseMoney(f.cost)
        val stock = if (!f.trackStock || f.stock.isBlank()) 0 else f.stock.trim().toIntOrNull()
        val low = if (f.lowStock.isBlank()) 5 else f.lowStock.trim().toIntOrNull()

        errors = ProductFormErrors(
            name = if (f.name.isBlank()) "Introduce un nombre" else null,
            price = if (price == null) "Introduce un precio válido" else null,
            cost = if (cost == null) "Coste no válido" else null,
            stock = if (stock == null || stock < 0) "Stock no válido" else null,
            lowStock = if (low == null || low < 0) "Valor no válido" else null
        )
        if (errors.hasAny) return

        val product = ProductEntity(
            name = f.name.trim(),
            categoryId = f.categoryId,
            taxRateId = f.taxRateId,
            sku = f.sku.trim().ifBlank { null },
            barcode = f.barcode.trim().ifBlank { null },
            priceCents = price ?: return,
            costCents = cost ?: return,
            stockQuantity = stock ?: return,
            lowStockThreshold = low ?: return,
            trackStock = f.trackStock
        )

        viewModelScope.launch {
            saving = true
            repo.create(product, userId = null)
                .onSuccess {
                    reset()
                    onSaved()
                }
                .onFailure {
                    saving = false
                    _messages.tryEmit(it.message ?: "No se pudo guardar el producto")
                }
        }
    }

    /** "2,50" or "2.5" -> 250 cents. Returns null if it is not a valid non-negative amount. */
    private fun parseMoney(text: String): Long? {
        val amount = text.trim().replace(',', '.').toBigDecimalOrNull() ?: return null
        if (amount.signum() < 0) return null
        return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
    }
}
