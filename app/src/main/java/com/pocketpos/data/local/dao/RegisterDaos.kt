package com.pocketpos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.pocketpos.data.local.entity.CashMovementEntity
import com.pocketpos.data.local.entity.CashMovementType
import com.pocketpos.data.local.entity.RegisterSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RegisterSessionDao {
    @Insert
    suspend fun insert(session: RegisterSessionEntity): Long

    @Update
    suspend fun update(session: RegisterSessionEntity)

    @Query("SELECT * FROM register_sessions WHERE id = :id")
    suspend fun getById(id: Long): RegisterSessionEntity?

    @Query("SELECT * FROM register_sessions WHERE status = 'OPEN' ORDER BY openedAt DESC LIMIT 1")
    suspend fun getOpen(): RegisterSessionEntity?

    @Query("SELECT * FROM register_sessions WHERE status = 'OPEN' ORDER BY openedAt DESC LIMIT 1")
    fun observeOpen(): Flow<RegisterSessionEntity?>

    @Query("SELECT * FROM register_sessions ORDER BY openedAt DESC")
    fun observeHistory(): Flow<List<RegisterSessionEntity>>
}

@Dao
interface CashMovementDao {
    @Insert
    suspend fun insert(movement: CashMovementEntity): Long

    @Query("SELECT * FROM cash_movements WHERE sessionId = :sessionId ORDER BY createdAt DESC")
    fun observeBySession(sessionId: Long): Flow<List<CashMovementEntity>>

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM cash_movements WHERE sessionId = :sessionId AND type = :type")
    suspend fun totalBySession(sessionId: Long, type: CashMovementType): Long
}
