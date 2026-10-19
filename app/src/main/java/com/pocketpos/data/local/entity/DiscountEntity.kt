package com.pocketpos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Preset discounts the cashier can pick.
 * PERCENT -> value in basis points (1000 = 10 %). FIXED -> value in cents.
 */
@Entity(tableName = "discounts")
data class DiscountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: DiscountType,
    val value: Long,
    val isActive: Boolean = true
)
