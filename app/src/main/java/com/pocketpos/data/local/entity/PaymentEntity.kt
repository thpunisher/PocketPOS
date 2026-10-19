package com.pocketpos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A sale can have several payments (split payment: part cash, part card).
 * amountCents = amount APPLIED to the sale (change is not included).
 * For cash: tenderedCents = what the customer handed over, changeCents = tendered - amount.
 */
@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("saleId"), Index("method")]
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val method: PaymentMethod,
    val amountCents: Long,
    val tenderedCents: Long? = null,
    val changeCents: Long = 0,
    val reference: String? = null,      // card terminal ticket, Bizum id...
    val createdAt: Long = System.currentTimeMillis()
)
