package com.pocketpos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categories", indices = [Index(value = ["name"], unique = true)])
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String? = null,       // e.g. "#6C5CE7" for the category chip
    val sortOrder: Int = 0,
    val isActive: Boolean = true
)
