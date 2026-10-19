package com.pocketpos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Single-row table (id is always 1): business data printed on receipts + receipt counter. */
@Entity(tableName = "business_settings")
data class BusinessSettingsEntity(
    @PrimaryKey val id: Long = 1,
    val businessName: String,
    val taxId: String? = null,          // CIF / NIF
    val address: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val currencyCode: String = "EUR",
    val receiptPrefix: String = "T",
    val nextReceiptNumber: Long = 1,
    val receiptFooter: String? = null
)
