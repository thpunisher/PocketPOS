package com.pocketpos.data.local.repository

import androidx.room.withTransaction
import com.pocketpos.data.local.PocketPosDatabase
import com.pocketpos.data.local.entity.AuditLogEntity
import com.pocketpos.data.local.entity.ProductEntity
import com.pocketpos.data.local.entity.StockMovementType

class ProductRepository(private val db: PocketPosDatabase) {

    private val productDao = db.productDao()
    private val ledger = StockLedger(db)

    /** Creates a product; if it starts with stock, an INITIAL ledger row explains it. */
    suspend fun create(product: ProductEntity, userId: Long?): Result<Long> = runCatching {
        db.withTransaction {
            if (product.name.isBlank()) throw PosException("El nombre es obligatorio")
            if (product.priceCents < 0) throw PosException("El precio no es válido")
            product.barcode?.let {
                if (productDao.countByBarcode(it) > 0) throw PosException("Ya existe un producto con ese código de barras")
            }
            product.sku?.let {
                if (productDao.countBySku(it) > 0) throw PosException("Ya existe un producto con esa referencia (SKU)")
            }

            val initialStock = if (product.trackStock) product.stockQuantity else 0
            val id = productDao.insert(product.copy(id = 0, stockQuantity = 0))
            if (initialStock > 0) {
                ledger.move(id, initialStock, StockMovementType.INITIAL, userId, note = "Stock inicial")
            }
            id
        }
    }

    /** Edits product data. Stock is NOT editable here: use InventoryRepository.adjustStock. */
    suspend fun update(product: ProductEntity, userId: Long?): Result<Unit> = runCatching {
        db.withTransaction {
            val old = productDao.getById(product.id) ?: throw PosException("Producto no encontrado")
            if (product.name.isBlank()) throw PosException("El nombre es obligatorio")

            productDao.update(
                product.copy(
                    stockQuantity = old.stockQuantity,
                    createdAt = old.createdAt,
                    updatedAt = System.currentTimeMillis()
                )
            )
            if (old.priceCents != product.priceCents) {
                db.auditLogDao().insert(
                    AuditLogEntity(
                        userId = userId,
                        action = "PRODUCT_PRICE",
                        entityType = "product",
                        entityId = old.id,
                        detail = "${old.priceCents} -> ${product.priceCents}"
                    )
                )
            }
        }
    }

    /** "Delete" = hide. The row stays so old tickets and stock history keep working. */
    suspend fun deactivate(productId: Long): Result<Unit> = runCatching {
        productDao.setActive(productId, false)
    }
}
