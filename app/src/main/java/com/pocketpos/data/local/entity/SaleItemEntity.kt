package com.pocketpos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One line of a ticket. Name, price, cost and tax rate are SNAPSHOTS: editing the
 * product later never changes past tickets. productId is null for "open price" lines
 * (a free-text item typed by the cashier) or if the product was ever deleted.
 */
@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("saleId"), Index("productId")]
)
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val productId: Long? = null,

    val productName: String,
    val unitPriceCents: Long,
    val unitCostCents: Long = 0,
    val quantity: Int,
    /** Discount on this line, in cents. */
    val discountCents: Long = 0,
    val taxRateBps: Int = 0,
    /** Tax contained in lineTotalCents. */
    val taxCents: Long = 0,
    /** unitPriceCents * quantity - discountCents */
    val lineTotalCents: Long
)
