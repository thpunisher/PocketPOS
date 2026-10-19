package com.pocketpos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.pocketpos.data.local.entity.RefundEntity
import com.pocketpos.data.local.entity.RefundItemEntity
import com.pocketpos.data.local.relation.RefundWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface RefundDao {
    @Insert
    suspend fun insert(refund: RefundEntity): Long

    @Insert
    suspend fun insertItems(items: List<RefundItemEntity>): List<Long>

    @Transaction
    @Query("SELECT * FROM refunds WHERE saleId = :saleId ORDER BY createdAt DESC")
    fun observeBySale(saleId: Long): Flow<List<RefundWithItems>>

    /** How many units of a sale line were already returned (to block over-refunds). */
    @Query("SELECT COALESCE(SUM(quantity), 0) FROM refund_items WHERE saleItemId = :saleItemId")
    suspend fun refundedQuantity(saleItemId: Long): Int

    @Query("SELECT COUNT(*) FROM refunds WHERE saleId = :saleId")
    suspend fun countForSale(saleId: Long): Int

    /** Cash handed back to customers during one shift (reduces the expected drawer cash). */
    @Query("SELECT COALESCE(SUM(totalCents), 0) FROM refunds WHERE sessionId = :sessionId AND method = 'CASH'")
    suspend fun cashRefundsTotal(sessionId: Long): Long

    @Query("SELECT COALESCE(SUM(totalCents), 0) FROM refunds WHERE createdAt BETWEEN :from AND :to")
    fun observeRefundTotal(from: Long, to: Long): Flow<Long>
}
