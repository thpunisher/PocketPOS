package com.pocketpos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A ticket. Money rules (all in cents, tax INCLUDED in prices):
 *   subtotalCents = sum of the lines' lineTotalCents (already after line discounts)
 *   totalCents    = subtotalCents - discountCents   (discountCents = sale-level discount)
 *   taxCents      = tax contained in totalCents
 */
@Entity(
    tableName = "sales",
    foreignKeys = [
        ForeignKey(
            entity = RegisterSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = DiscountEntity::class,
            parentColumns = ["id"],
            childColumns = ["discountId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["voidedByUserId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("sessionId"),
        Index("userId"),
        Index("customerId"),
        Index("discountId"),
        Index("voidedByUserId"),
        Index("status"),
        Index("completedAt"),
        Index(value = ["receiptNumber"], unique = true)
    ]
)
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    /** null while the ticket is parked (OPEN); assigned on checkout, e.g. "T-000042". */
    val receiptNumber: String? = null,
    val status: SaleStatus = SaleStatus.OPEN,

    val sessionId: Long,
    val userId: Long,
    val customerId: Long? = null,
    val discountId: Long? = null,

    val subtotalCents: Long = 0,
    val discountCents: Long = 0,
    val taxCents: Long = 0,
    val totalCents: Long = 0,

    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,

    val voidedAt: Long? = null,
    val voidedByUserId: Long? = null,
    val voidReason: String? = null
)
