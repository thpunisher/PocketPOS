package com.pocketpos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Money put into / taken out of the drawer outside of sales (petty cash, supplier paid in cash...). */
@Entity(
    tableName = "cash_movements",
    foreignKeys = [
        ForeignKey(
            entity = RegisterSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("sessionId"), Index("userId")]
)
data class CashMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val userId: Long,
    val type: CashMovementType,
    val amountCents: Long,
    val reason: String,
    val createdAt: Long = System.currentTimeMillis()
)
