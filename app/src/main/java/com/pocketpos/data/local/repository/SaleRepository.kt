package com.pocketpos.data.local.repository

import androidx.room.withTransaction
import com.pocketpos.data.local.PocketPosDatabase
import com.pocketpos.data.local.entity.AuditLogEntity
import com.pocketpos.data.local.entity.DiscountType
import com.pocketpos.data.local.entity.PaymentEntity
import com.pocketpos.data.local.entity.PaymentMethod
import com.pocketpos.data.local.entity.RefundEntity
import com.pocketpos.data.local.entity.RefundItemEntity
import com.pocketpos.data.local.entity.RegisterStatus
import com.pocketpos.data.local.entity.SaleEntity
import com.pocketpos.data.local.entity.SaleItemEntity
import com.pocketpos.data.local.entity.SaleStatus
import com.pocketpos.data.local.entity.StockMovementType
import com.pocketpos.data.local.entity.UserRole
import java.util.Locale

/** One line of the cart on screen. productId = null means an "open price" line. */
data class CartLine(
    val productId: Long?,
    val quantity: Int,
    val discountCents: Long = 0,
    /** Only for open-price lines (productId == null): */
    val name: String? = null,
    val unitPriceCents: Long? = null
)

data class PaymentInput(
    val method: PaymentMethod,
    /** Amount applied to the sale. */
    val amountCents: Long,
    /** Cash only: what the customer handed over (change is computed). Defaults to amountCents. */
    val tenderedCents: Long? = null,
    val reference: String? = null
)

data class CheckoutRequest(
    val sessionId: Long,
    val userId: Long,
    val lines: List<CartLine>,
    val payments: List<PaymentInput>,
    val customerId: Long? = null,
    /** Preset discount (percent or fixed). If null, manualDiscountCents is used. */
    val discountId: Long? = null,
    val manualDiscountCents: Long = 0,
    val note: String? = null,
    /** If the cart came from a parked ticket, pass its id so it is removed. */
    val parkedSaleId: Long? = null
)

data class RefundLine(
    val saleItemId: Long,
    val quantity: Int,
    val restock: Boolean = true
)

class SaleRepository(private val db: PocketPosDatabase) {

    private val saleDao = db.saleDao()
    private val productDao = db.productDao()
    private val taxDao = db.taxRateDao()
    private val discountDao = db.discountDao()
    private val settingsDao = db.businessSettingsDao()
    private val sessionDao = db.registerSessionDao()
    private val refundDao = db.refundDao()
    private val userDao = db.userDao()
    private val auditDao = db.auditLogDao()
    private val ledger = StockLedger(db)

    // =====================================================================
    //  CHECKOUT  (cobrar)
    // =====================================================================

    /** Completes a sale: totals, receipt number, payments and stock, all or nothing. Returns the sale id. */
    suspend fun checkout(request: CheckoutRequest): Result<Long> = runCatching {
        db.withTransaction {
            requireOpenSession(request.sessionId)

            val items = buildItems(request.lines)
            val subtotal = items.sumOf { it.lineTotalCents }
            val discount = resolveSaleDiscount(request.discountId, request.manualDiscountCents, subtotal)
            val total = subtotal - discount
            val tax = proportionalTax(items.sumOf { it.taxCents }, subtotal, total)

            // ---- payments must cover the total exactly (change is tracked separately) ----
            val paid = request.payments.sumOf { it.amountCents }
            if (request.payments.any { it.amountCents <= 0 }) throw PosException("Importe de pago no válido")
            if (paid != total) throw PosException("El pago ($paid) no coincide con el total ($total)")

            // ---- stock check (same product can appear in several lines) ----
            val needed = items.filter { it.productId != null }
                .groupBy { it.productId!! }
                .mapValues { entry -> entry.value.sumOf { it.quantity } }
            val products = productDao.getByIds(needed.keys.toList()).associateBy { it.id }
            for ((productId, qty) in needed) {
                val product = products.getValue(productId)
                if (product.trackStock && product.stockQuantity < qty) {
                    throw PosException("Stock insuficiente de «${product.name}» (quedan ${product.stockQuantity})")
                }
            }

            // ---- replace the parked ticket, if any ----
            request.parkedSaleId?.let { saleDao.deleteParked(it) }

            // ---- receipt number ----
            val settings = settingsDao.get() ?: throw PosException("Faltan los datos del negocio")
            settingsDao.incrementReceiptCounter()
            val receipt = "%s-%06d".format(Locale.ROOT, settings.receiptPrefix, settings.nextReceiptNumber)

            val now = System.currentTimeMillis()
            val saleId = saleDao.insert(
                SaleEntity(
                    receiptNumber = receipt,
                    status = SaleStatus.COMPLETED,
                    sessionId = request.sessionId,
                    userId = request.userId,
                    customerId = request.customerId,
                    discountId = request.discountId,
                    subtotalCents = subtotal,
                    discountCents = discount,
                    taxCents = tax,
                    totalCents = total,
                    note = request.note,
                    createdAt = now,
                    completedAt = now
                )
            )

            saleDao.insertItems(items.map { it.copy(saleId = saleId) })

            saleDao.insertPayments(
                request.payments.map { p ->
                    val tendered = if (p.method == PaymentMethod.CASH) p.tenderedCents ?: p.amountCents else null
                    if (tendered != null && tendered < p.amountCents) {
                        throw PosException("El efectivo entregado no cubre el importe")
                    }
                    PaymentEntity(
                        saleId = saleId,
                        method = p.method,
                        amountCents = p.amountCents,
                        tenderedCents = tendered,
                        changeCents = if (tendered != null) tendered - p.amountCents else 0,
                        reference = p.reference,
                        createdAt = now
                    )
                }
            )

            items.forEach { item ->
                val productId = item.productId
                if (productId != null) {
                    ledger.move(productId, -item.quantity, StockMovementType.SALE, request.userId, saleId = saleId)
                }
            }

            saleId
        }
    }

    // =====================================================================
    //  PARKED TICKETS  (tickets en espera)
    // =====================================================================

    /** Saves the cart as an OPEN ticket: no payment, no stock change, no receipt number. */
    suspend fun parkSale(
        sessionId: Long,
        userId: Long,
        lines: List<CartLine>,
        customerId: Long? = null,
        note: String? = null,
        replaceParkedId: Long? = null
    ): Result<Long> = runCatching {
        db.withTransaction {
            requireOpenSession(sessionId)
            val items = buildItems(lines)
            val subtotal = items.sumOf { it.lineTotalCents }

            replaceParkedId?.let { saleDao.deleteParked(it) }

            val saleId = saleDao.insert(
                SaleEntity(
                    status = SaleStatus.OPEN,
                    sessionId = sessionId,
                    userId = userId,
                    customerId = customerId,
                    subtotalCents = subtotal,
                    taxCents = items.sumOf { it.taxCents },
                    totalCents = subtotal,
                    note = note
                )
            )
            saleDao.insertItems(items.map { it.copy(saleId = saleId) })
            saleId
        }
    }

    /** Turns a parked ticket back into cart lines (prices are re-read from the products on checkout). */
    suspend fun resumeLines(parkedSaleId: Long): List<CartLine> =
        saleDao.getItems(parkedSaleId).map {
            CartLine(
                productId = it.productId,
                quantity = it.quantity,
                discountCents = it.discountCents,
                name = it.productName,
                unitPriceCents = it.unitPriceCents
            )
        }

    suspend fun discardParked(parkedSaleId: Long) = saleDao.deleteParked(parkedSaleId)

    // =====================================================================
    //  VOID  (anular)  — same shift only, whole ticket, manager/owner
    // =====================================================================

    suspend fun voidSale(saleId: Long, userId: Long, reason: String): Result<Unit> = runCatching {
        db.withTransaction {
            val user = userDao.getById(userId) ?: throw PosException("Usuario no encontrado")
            if (user.role == UserRole.CASHIER) throw PosException("Solo un encargado puede anular ventas")
            if (reason.isBlank()) throw PosException("Indica el motivo de la anulación")

            val sale = saleDao.getById(saleId) ?: throw PosException("Venta no encontrada")
            if (sale.status != SaleStatus.COMPLETED) throw PosException("Solo se pueden anular ventas completadas")
            if (refundDao.countForSale(saleId) > 0) throw PosException("La venta ya tiene devoluciones")

            val session = sessionDao.getById(sale.sessionId)
            if (session == null || session.status != RegisterStatus.OPEN) {
                throw PosException("La caja de esta venta ya está cerrada: haz una devolución")
            }

            saleDao.update(
                sale.copy(
                    status = SaleStatus.VOIDED,
                    voidedAt = System.currentTimeMillis(),
                    voidedByUserId = userId,
                    voidReason = reason
                )
            )

            saleDao.getItems(saleId).forEach { item ->
                val productId = item.productId
                if (productId != null) {
                    ledger.move(productId, item.quantity, StockMovementType.VOID, userId, saleId = saleId)
                }
            }

            auditDao.insert(
                AuditLogEntity(
                    userId = userId,
                    action = "SALE_VOID",
                    entityType = "sale",
                    entityId = saleId,
                    detail = "${sale.receiptNumber}: $reason"
                )
            )
        }
    }

    // =====================================================================
    //  REFUND  (devolución) — partial, repeatable, any later shift
    // =====================================================================

    suspend fun refund(
        saleId: Long,
        sessionId: Long,
        userId: Long,
        lines: List<RefundLine>,
        method: PaymentMethod,
        reason: String? = null
    ): Result<Long> = runCatching {
        db.withTransaction {
            val sale = saleDao.getById(saleId) ?: throw PosException("Venta no encontrada")
            if (sale.status != SaleStatus.COMPLETED) throw PosException("Solo se pueden devolver ventas completadas")
            requireOpenSession(sessionId)
            if (lines.isEmpty()) throw PosException("Selecciona al menos un artículo")
            if (lines.map { it.saleItemId }.distinct().size != lines.size) throw PosException("Línea repetida")

            val prepared = lines.map { line ->
                val item = saleDao.getItem(line.saleItemId) ?: throw PosException("Línea no encontrada")
                if (item.saleId != saleId) throw PosException("La línea no pertenece a esta venta")
                if (line.quantity <= 0) throw PosException("Cantidad no válida")
                if (line.quantity + refundDao.refundedQuantity(item.id) > item.quantity) {
                    throw PosException("No puedes devolver más de lo vendido en «${item.productName}»")
                }
                // proportional share of the line total (keeps line discounts)
                val amount = (item.lineTotalCents * line.quantity + item.quantity / 2) / item.quantity
                Triple(item, line, amount)
            }

            val refundId = refundDao.insert(
                RefundEntity(
                    saleId = saleId,
                    sessionId = sessionId,
                    userId = userId,
                    method = method,
                    totalCents = prepared.sumOf { it.third },
                    reason = reason
                )
            )
            refundDao.insertItems(
                prepared.map { (item, line, amount) ->
                    RefundItemEntity(
                        refundId = refundId,
                        saleItemId = item.id,
                        quantity = line.quantity,
                        amountCents = amount,
                        restock = line.restock
                    )
                }
            )
            prepared.forEach { (item, line, _) ->
                val productId = item.productId
                if (line.restock && productId != null) {
                    ledger.move(productId, line.quantity, StockMovementType.REFUND, userId, refundId = refundId)
                }
            }

            auditDao.insert(
                AuditLogEntity(
                    userId = userId,
                    action = "REFUND",
                    entityType = "sale",
                    entityId = saleId,
                    detail = "refund #$refundId"
                )
            )
            refundId
        }
    }

    // =====================================================================
    //  internals
    // =====================================================================

    private suspend fun requireOpenSession(sessionId: Long) {
        val session = sessionDao.getById(sessionId)
        if (session == null || session.status != RegisterStatus.OPEN) {
            throw PosException("La caja está cerrada")
        }
    }

    /** Snapshots product data into sale lines and extracts the tax contained in each price. */
    private suspend fun buildItems(lines: List<CartLine>): List<SaleItemEntity> {
        if (lines.isEmpty()) throw PosException("El ticket está vacío")

        val defaultRateBps = taxDao.getDefault()?.rateBps ?: 0
        val products = productDao
            .getByIds(lines.mapNotNull { it.productId }.distinct())
            .associateBy { it.id }
        val rateCache = HashMap<Long, Int>()

        return lines.map { line ->
            if (line.quantity <= 0) throw PosException("Cantidad no válida")

            val product = line.productId?.let { id ->
                products[id] ?: throw PosException("Producto no encontrado")
            }
            if (product != null && !product.isActive) {
                throw PosException("«${product.name}» está desactivado")
            }

            val name = product?.name ?: line.name ?: throw PosException("Falta el nombre del artículo")
            val price = product?.priceCents ?: line.unitPriceCents ?: throw PosException("Falta el precio")
            if (price < 0) throw PosException("Precio no válido")

            val rateBps = product?.taxRateId?.let { id ->
                rateCache.getOrPut(id) { taxDao.getById(id)?.rateBps ?: defaultRateBps }
            } ?: defaultRateBps

            val gross = price * line.quantity
            val discount = line.discountCents.coerceIn(0L, gross)
            val lineTotal = gross - discount

            SaleItemEntity(
                saleId = 0,
                productId = product?.id,
                productName = name,
                unitPriceCents = price,
                unitCostCents = product?.costCents ?: 0,
                quantity = line.quantity,
                discountCents = discount,
                taxRateBps = rateBps,
                taxCents = taxIncluded(lineTotal, rateBps),
                lineTotalCents = lineTotal
            )
        }
    }

    private suspend fun resolveSaleDiscount(discountId: Long?, manualCents: Long, subtotal: Long): Long {
        val raw = if (discountId != null) {
            val preset = discountDao.getById(discountId)
            if (preset == null || !preset.isActive) throw PosException("Descuento no disponible")
            when (preset.type) {
                DiscountType.PERCENT -> (subtotal * preset.value + 5_000) / 10_000
                DiscountType.FIXED -> preset.value
            }
        } else {
            manualCents
        }
        return raw.coerceIn(0L, subtotal)
    }

    /** Tax already contained in a tax-inclusive amount: gross * rate / (1 + rate), rounded. */
    private fun taxIncluded(grossCents: Long, rateBps: Int): Long =
        (grossCents * rateBps + (10_000L + rateBps) / 2) / (10_000L + rateBps)

    /** After a sale-level discount, tax shrinks in the same proportion as the total. */
    private fun proportionalTax(itemsTax: Long, subtotal: Long, total: Long): Long =
        if (subtotal == 0L) 0L else (itemsTax * total + subtotal / 2) / subtotal
}
