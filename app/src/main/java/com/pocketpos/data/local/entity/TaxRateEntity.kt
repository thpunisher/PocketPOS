package com.pocketpos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** rateBps = basis points: 2100 = 21 %, 1000 = 10 %, 400 = 4 %. Prices already INCLUDE tax. */
@Entity(tableName = "tax_rates")
data class TaxRateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val rateBps: Int,
    val isDefault: Boolean = false,
    val isActive: Boolean = true
)
