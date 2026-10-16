package com.pocketpos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    val category: String = "",

    val priceCents: Long,

    val stockQuantity: Int = 0,

    val barcode: String? = null,

    val isActive: Boolean = true
)