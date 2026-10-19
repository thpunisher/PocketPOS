package com.pocketpos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.pocketpos.data.local.entity.AuditLogEntity
import com.pocketpos.data.local.entity.PurchaseOrderEntity
import com.pocketpos.data.local.entity.PurchaseOrderItemEntity
import com.pocketpos.data.local.entity.PurchaseStatus
import com.pocketpos.data.local.entity.StockMovementEntity
import com.pocketpos.data.local.relation.PurchaseOrderWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface StockMovementDao {
    @Insert
    suspend fun insert(movement: StockMovementEntity): Long

    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY createdAt DESC, id DESC")
    fun observeByProduct(productId: Long): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements ORDER BY createdAt DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<StockMovementEntity>>
}

@Dao
interface PurchaseOrderDao {
    @Insert
    suspend fun insert(order: PurchaseOrderEntity): Long

    @Update
    suspend fun update(order: PurchaseOrderEntity)

    @Insert
    suspend fun insertItems(items: List<PurchaseOrderItemEntity>)

    @Update
    suspend fun updateItems(items: List<PurchaseOrderItemEntity>)

    @Query("SELECT * FROM purchase_orders WHERE id = :id")
    suspend fun getById(id: Long): PurchaseOrderEntity?

    @Query("SELECT * FROM purchase_order_items WHERE purchaseOrderId = :orderId")
    suspend fun getItems(orderId: Long): List<PurchaseOrderItemEntity>

    @Transaction
    @Query("SELECT * FROM purchase_orders WHERE id = :id")
    fun observeDetails(id: Long): Flow<PurchaseOrderWithItems?>

    @Transaction
    @Query("SELECT * FROM purchase_orders WHERE (:status IS NULL OR status = :status) ORDER BY createdAt DESC")
    fun observeByStatus(status: PurchaseStatus? = null): Flow<List<PurchaseOrderWithItems>>
}

@Dao
interface AuditLogDao {
    @Insert
    suspend fun insert(log: AuditLogEntity): Long

    @Query("SELECT * FROM audit_logs ORDER BY createdAt DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int = 200): Flow<List<AuditLogEntity>>
}
