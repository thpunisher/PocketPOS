package com.pocketpos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "refund_items",
    foreignKeys = [
        ForeignKey(
            entity = RefundEntity::class,
            parentColumns = ["id"],
            childColumns = ["refundId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SaleItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleItemId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("refundId"), Index("saleItemId")]
)
data class RefundItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val refundId: Long,
    val saleItemId: Long,
    val quantity: Int,
    val amountCents: Long,
    /** true = item goes back to stock; false = damaged / not resellable. */
    val restock: Boolean = true
)
