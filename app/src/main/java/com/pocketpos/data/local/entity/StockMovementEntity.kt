package com.pocketpos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Append-only stock ledger: every change to products.stockQuantity writes one row here,
 * so the stock number can always be explained (and rebuilt) from history.
 * Exactly one of saleId / refundId / purchaseOrderId is normally filled, depending on type.
 */
@Entity(
    tableName = "stock_movements",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = RefundEntity::class,
            parentColumns = ["id"],
            childColumns = ["refundId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = PurchaseOrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchaseOrderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("productId"),
        Index("userId"),
        Index("saleId"),
        Index("refundId"),
        Index("purchaseOrderId"),
        Index("createdAt")
    ]
)
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val type: StockMovementType,
    /** Positive = stock in, negative = stock out. */
    val quantityChange: Int,
    /** Stock level right after this movement. */
    val stockAfter: Int,
    val userId: Long? = null,
    val saleId: Long? = null,
    val refundId: Long? = null,
    val purchaseOrderId: Long? = null,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
