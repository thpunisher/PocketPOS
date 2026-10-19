package com.pocketpos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One cash-register shift ("turno" / "caja"): open with a float, close with a count. */
@Entity(
    tableName = "register_sessions",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["openedByUserId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["closedByUserId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("openedByUserId"), Index("closedByUserId"), Index("status")]
)
data class RegisterSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val openedByUserId: Long,
    val closedByUserId: Long? = null,
    val status: RegisterStatus = RegisterStatus.OPEN,
    val openedAt: Long = System.currentTimeMillis(),
    val closedAt: Long? = null,
    val openingFloatCents: Long,
    /** Filled when closing: what the system says should be in the drawer. */
    val expectedCashCents: Long? = null,
    /** Filled when closing: what the cashier actually counted. */
    val countedCashCents: Long? = null,
    val note: String? = null
)
