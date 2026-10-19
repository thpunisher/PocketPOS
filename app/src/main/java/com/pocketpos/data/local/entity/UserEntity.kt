package com.pocketpos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Staff who use the app. Never hard-delete (sales reference them): set isActive = false. */
@Entity(tableName = "users", indices = [Index(value = ["name"], unique = true)])
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val pinHash: String,
    val role: UserRole = UserRole.CASHIER,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
