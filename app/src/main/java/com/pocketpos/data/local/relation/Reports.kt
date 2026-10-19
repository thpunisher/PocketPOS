package com.pocketpos.data.local.relation

import com.pocketpos.data.local.entity.PaymentMethod

data class TopProduct(
    val productName: String,
    val unitsSold: Long,
    val revenueCents: Long
)

data class PaymentTotal(
    val method: PaymentMethod,
    val totalCents: Long
)

data class DailyRevenue(
    /** yyyy-MM-dd, local time */
    val day: String,
    val salesCount: Int,
    val revenueCents: Long
)

data class CategoryRevenue(
    val categoryName: String?,
    val unitsSold: Long,
    val revenueCents: Long
)
