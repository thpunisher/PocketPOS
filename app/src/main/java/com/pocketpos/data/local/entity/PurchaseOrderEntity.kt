package com.pocketpos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Restocking from a supplier ("pedido / albarán de compra"). */
@Entity(
    tableName = "purchase_orders",
    foreignKeys = [
        ForeignKey(
            entity = SupplierEntity::class,
            parentColumns = ["id"],
            childColumns = ["supplierId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["createdByUserId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("supplierId"), Index("createdByUserId"), Index("status")]
)
data class PurchaseOrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val supplierId: Long,
    val createdByUserId: Long,
    val status: PurchaseStatus = PurchaseStatus.DRAFT,
    /** Supplier's delivery-note / invoice number. */
    val reference: String? = null,
    val totalCents: Long = 0,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val orderedAt: Long? = null,
    val receivedAt: Long? = null
)
