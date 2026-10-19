package com.pocketpos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val contactName: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val taxId: String? = null,
    val notes: String? = null,
    val isActive: Boolean = true
)
