package com.pocketpos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = TaxRateEntity::class,
            parentColumns = ["id"],
            childColumns = ["taxRateId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("categoryId"),
        Index("taxRateId"),
        Index("name"),
        Index(value = ["barcode"], unique = true),
        Index(value = ["sku"], unique = true)
    ]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val categoryId: Long? = null,
    /** null = use the default tax rate */
    val taxRateId: Long? = null,

    val name: String,
    val sku: String? = null,
    val barcode: String? = null,

    /** Selling price in cents, tax included. */
    val priceCents: Long,
    /** Last known purchase cost in cents (for profit reports). */
    val costCents: Long = 0,

    val stockQuantity: Int = 0,
    /** "Stock bajo" when stockQuantity is between 1 and this value. */
    val lowStockThreshold: Int = 5,
    /** false for services / items without stock control. */
    val trackStock: Boolean = true,

    val imageUri: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
