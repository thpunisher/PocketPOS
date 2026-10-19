package com.pocketpos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A return ("devolución") against a completed sale. Can be partial and can happen several times. */
@Entity(
    tableName = "refunds",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.RESTRICT
        ),
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
        )
    ],
    indices = [Index("saleId"), Index("sessionId"), Index("userId"), Index("createdAt")]
)
data class RefundEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    /** Session in which the money was given back (affects that drawer's cash count). */
    val sessionId: Long,
    val userId: Long,
    val method: PaymentMethod,
    val totalCents: Long,
    val reason: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
