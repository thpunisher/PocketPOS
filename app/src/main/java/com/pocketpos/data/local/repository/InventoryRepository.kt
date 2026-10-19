package com.pocketpos.data.local.repository

import androidx.room.withTransaction
import com.pocketpos.data.local.PocketPosDatabase
import com.pocketpos.data.local.entity.AuditLogEntity
import com.pocketpos.data.local.entity.PurchaseOrderEntity
import com.pocketpos.data.local.entity.PurchaseOrderItemEntity
import com.pocketpos.data.local.entity.PurchaseStatus
import com.pocketpos.data.local.entity.StockMovementType

data class PurchaseLine(
    val productId: Long,
    val quantity: Int,
    val unitCostCents: Long
)

/** Manual stock corrections and restocking from suppliers. */
class InventoryRepository(private val db: PocketPosDatabase) {

    private val productDao = db.productDao()
    private val orderDao = db.purchaseOrderDao()
    private val auditDao = db.auditLogDao()
    private val ledger = StockLedger(db)

    /** Count correction (ADJUSTMENT), breakage / expiry (WASTE). delta can be negative. */
    suspend fun adjustStock(
        productId: Long,
        delta: Int,
        userId: Long,
        type: StockMovementType = StockMovementType.ADJUSTMENT,
        note: String? = null
    ): Result<Unit> = runCatching {
        db.withTransaction {
            if (type != StockMovementType.ADJUSTMENT && type != StockMovementType.WASTE) {
                throw PosException("Tipo de movimiento no permitido")
            }
            if (delta == 0) throw PosException("La cantidad no puede ser 0")

            val product = productDao.getById(productId) ?: throw PosException("Producto no encontrado")
            if (!product.trackStock) throw PosException("Este producto no controla stock")
            if (product.stockQuantity + delta < 0) throw PosException("El stock no puede ser negativo")

            ledger.move(productId, delta, type, userId, note = note)
            auditDao.insert(
                AuditLogEntity(
                    userId = userId,
                    action = "STOCK_ADJUST",
                    entityType = "product",
                    entityId = productId,
                    detail = "$type $delta"
                )
            )
        }
    }

    suspend fun createPurchaseOrder(
        supplierId: Long,
        userId: Long,
        lines: List<PurchaseLine>,
        reference: String? = null,
        note: String? = null,
        placeOrderNow: Boolean = false
    ): Result<Long> = runCatching {
        db.withTransaction {
            if (lines.isEmpty()) throw PosException("El pedido está vacío")
            if (lines.any { it.quantity <= 0 || it.unitCostCents < 0 }) throw PosException("Línea no válida")

            val now = System.currentTimeMillis()
            val orderId = orderDao.insert(
                PurchaseOrderEntity(
                    supplierId = supplierId,
                    createdByUserId = userId,
                    status = if (placeOrderNow) PurchaseStatus.ORDERED else PurchaseStatus.DRAFT,
                    reference = reference,
                    totalCents = lines.sumOf { it.unitCostCents * it.quantity },
                    note = note,
                    orderedAt = if (placeOrderNow) now else null
                )
            )
            orderDao.insertItems(
                lines.map {
                    PurchaseOrderItemEntity(
                        purchaseOrderId = orderId,
                        productId = it.productId,
                        quantityOrdered = it.quantity,
                        unitCostCents = it.unitCostCents
                    )
                }
            )
            orderId
        }
    }

    suspend fun markOrdered(orderId: Long): Result<Unit> = runCatching {
        db.withTransaction {
            val order = orderDao.getById(orderId) ?: throw PosException("Pedido no encontrado")
            if (order.status != PurchaseStatus.DRAFT) throw PosException("El pedido ya fue enviado")
            orderDao.update(order.copy(status = PurchaseStatus.ORDERED, orderedAt = System.currentTimeMillis()))
        }
    }

    suspend fun cancelOrder(orderId: Long): Result<Unit> = runCatching {
        db.withTransaction {
            val order = orderDao.getById(orderId) ?: throw PosException("Pedido no encontrado")
            if (order.status == PurchaseStatus.RECEIVED) throw PosException("El pedido ya fue recibido")
            orderDao.update(order.copy(status = PurchaseStatus.CANCELLED))
        }
    }

    /**
     * Receives goods. receivedByItemId = quantities actually delivered per order line
     * (partial deliveries allowed); null = receive everything still pending.
     * Updates stock, the product's last cost, and closes the order when fully received.
     */
    suspend fun receiveOrder(
        orderId: Long,
        userId: Long,
        receivedByItemId: Map<Long, Int>? = null
    ): Result<Unit> = runCatching {
        db.withTransaction {
            val order = orderDao.getById(orderId) ?: throw PosException("Pedido no encontrado")
            if (order.status != PurchaseStatus.DRAFT && order.status != PurchaseStatus.ORDERED) {
                throw PosException("El pedido no se puede recibir")
            }

            val items = orderDao.getItems(orderId)
            val updated = items.map { item ->
                val qty = receivedByItemId?.get(item.id) ?: (item.quantityOrdered - item.quantityReceived)
                if (qty < 0) throw PosException("Cantidad recibida no válida")
                if (qty > 0) {
                    ledger.move(
                        productId = item.productId,
                        delta = qty,
                        type = StockMovementType.PURCHASE,
                        userId = userId,
                        purchaseOrderId = orderId
                    )
                    productDao.updateCost(item.productId, item.unitCostCents)
                }
                item.copy(quantityReceived = item.quantityReceived + qty)
            }
            orderDao.updateItems(updated)

            val complete = updated.all { it.quantityReceived >= it.quantityOrdered }
            orderDao.update(
                order.copy(
                    status = if (complete) PurchaseStatus.RECEIVED else PurchaseStatus.ORDERED,
                    receivedAt = if (complete) System.currentTimeMillis() else order.receivedAt
                )
            )
        }
    }
}
