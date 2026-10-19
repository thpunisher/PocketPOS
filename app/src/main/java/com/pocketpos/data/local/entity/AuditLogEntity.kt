package com.pocketpos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Who did what: voids, refunds, price changes, register open/close, manual stock adjustments... */
@Entity(
    tableName = "audit_logs",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("userId"), Index("createdAt"), Index("entityType", "entityId")]
)
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long? = null,
    /** e.g. "SALE_VOID", "REFUND", "REGISTER_CLOSE", "STOCK_ADJUST", "PRODUCT_PRICE" */
    val action: String,
    /** e.g. "sale", "product", "register_session" */
    val entityType: String,
    val entityId: Long? = null,
    val detail: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
