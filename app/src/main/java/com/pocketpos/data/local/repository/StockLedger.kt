package com.pocketpos.data.local.repository

import com.pocketpos.data.local.PocketPosDatabase
import com.pocketpos.data.local.entity.StockMovementEntity
import com.pocketpos.data.local.entity.StockMovementType

/**
 * The ONLY place that changes products.stockQuantity: it updates the number and writes the
 * matching stock_movements row. Always call it inside db.withTransaction { }.
 */
class StockLedger(private val db: PocketPosDatabase) {

    suspend fun move(
        productId: Long,
        delta: Int,
        type: StockMovementType,
        userId: Long?,
        saleId: Long? = null,
        refundId: Long? = null,
        purchaseOrderId: Long? = null,
        note: String? = null
    ) {
        val product = db.productDao().getById(productId)
            ?: throw PosException("Producto no encontrado")

        // Services / items without stock control never touch the ledger.
        if (!product.trackStock) return

        db.productDao().adjustStock(productId, delta)
        db.stockMovementDao().insert(
            StockMovementEntity(
                productId = productId,
                type = type,
                quantityChange = delta,
                stockAfter = product.stockQuantity + delta,
                userId = userId,
                saleId = saleId,
                refundId = refundId,
                purchaseOrderId = purchaseOrderId,
                note = note
            )
        )
    }
}
