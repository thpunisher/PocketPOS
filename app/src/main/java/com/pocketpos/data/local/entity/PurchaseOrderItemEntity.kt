package com.pocketpos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "purchase_order_items",
    foreignKeys = [
        ForeignKey(
            entity = PurchaseOrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchaseOrderId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("purchaseOrderId"), Index("productId")]
)
data class PurchaseOrderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val purchaseOrderId: Long,
    val productId: Long,
    val quantityOrdered: Int,
    val quantityReceived: Int = 0,
    val unitCostCents: Long
)
