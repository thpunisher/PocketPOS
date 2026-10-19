package com.pocketpos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.pocketpos.data.local.entity.PaymentEntity
import com.pocketpos.data.local.entity.SaleEntity
import com.pocketpos.data.local.entity.SaleItemEntity
import com.pocketpos.data.local.relation.CategoryRevenue
import com.pocketpos.data.local.relation.DailyRevenue
import com.pocketpos.data.local.relation.PaymentTotal
import com.pocketpos.data.local.relation.SaleWithDetails
import com.pocketpos.data.local.relation.TopProduct
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {

    // ---------- Write ----------
    @Insert
    suspend fun insert(sale: SaleEntity): Long

    @Update
    suspend fun update(sale: SaleEntity)

    @Insert
    suspend fun insertItems(items: List<SaleItemEntity>): List<Long>

    @Insert
    suspend fun insertPayments(payments: List<PaymentEntity>)

    /** Items and payments are removed by ON DELETE CASCADE. Only used for parked tickets. */
    @Query("DELETE FROM sales WHERE id = :id AND status = 'OPEN'")
    suspend fun deleteParked(id: Long)

    // ---------- Read ----------
    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getById(id: Long): SaleEntity?

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getItems(saleId: Long): List<SaleItemEntity>

    @Query("SELECT * FROM sale_items WHERE id = :id")
    suspend fun getItem(id: Long): SaleItemEntity?

    @Transaction
    @Query("SELECT * FROM sales WHERE id = :id")
    fun observeDetails(id: Long): Flow<SaleWithDetails?>

    @Transaction
    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getDetails(id: Long): SaleWithDetails?

    /** Tickets on hold ("en espera"). */
    @Transaction
    @Query("SELECT * FROM sales WHERE status = 'OPEN' ORDER BY createdAt DESC")
    fun observeParked(): Flow<List<SaleWithDetails>>

    /** Sales history screen (completed + voided), optionally filtered by receipt number. */
    @Transaction
    @Query(
        """
        SELECT * FROM sales
        WHERE status != 'OPEN'
          AND completedAt BETWEEN :from AND :to
          AND (:receipt = '' OR receiptNumber LIKE '%' || :receipt || '%')
        ORDER BY completedAt DESC
        """
    )
    fun observeHistory(from: Long, to: Long, receipt: String = ""): Flow<List<SaleWithDetails>>

    @Transaction
    @Query("SELECT * FROM sales WHERE sessionId = :sessionId AND status != 'OPEN' ORDER BY completedAt DESC")
    fun observeBySession(sessionId: Long): Flow<List<SaleWithDetails>>

    @Transaction
    @Query("SELECT * FROM sales WHERE customerId = :customerId AND status = 'COMPLETED' ORDER BY completedAt DESC")
    fun observeByCustomer(customerId: Long): Flow<List<SaleWithDetails>>

    // ---------- Register / dashboard totals ----------
    @Query(
        """
        SELECT COALESCE(SUM(totalCents), 0) FROM sales
        WHERE status = 'COMPLETED' AND completedAt BETWEEN :from AND :to
        """
    )
    fun observeRevenue(from: Long, to: Long): Flow<Long>

    @Query(
        """
        SELECT COUNT(*) FROM sales
        WHERE status = 'COMPLETED' AND completedAt BETWEEN :from AND :to
        """
    )
    fun observeSalesCount(from: Long, to: Long): Flow<Int>

    /** Cash received for sales of one shift (change already excluded). */
    @Query(
        """
        SELECT COALESCE(SUM(p.amountCents), 0)
        FROM payments p JOIN sales s ON s.id = p.saleId
        WHERE s.sessionId = :sessionId AND s.status = 'COMPLETED' AND p.method = 'CASH'
        """
    )
    suspend fun cashPaymentsTotal(sessionId: Long): Long

    @Query(
        """
        SELECT p.method AS method, SUM(p.amountCents) AS totalCents
        FROM payments p JOIN sales s ON s.id = p.saleId
        WHERE s.sessionId = :sessionId AND s.status = 'COMPLETED'
        GROUP BY p.method
        """
    )
    fun observePaymentTotalsBySession(sessionId: Long): Flow<List<PaymentTotal>>

    // ---------- Reports ----------
    @Query(
        """
        SELECT si.productName AS productName,
               SUM(si.quantity) AS unitsSold,
               SUM(si.lineTotalCents) AS revenueCents
        FROM sale_items si JOIN sales s ON s.id = si.saleId
        WHERE s.status = 'COMPLETED' AND s.completedAt BETWEEN :from AND :to
        GROUP BY si.productId, si.productName
        ORDER BY unitsSold DESC
        LIMIT :limit
        """
    )
    fun observeTopProducts(from: Long, to: Long, limit: Int = 10): Flow<List<TopProduct>>

    @Query(
        """
        SELECT strftime('%Y-%m-%d', s.completedAt / 1000, 'unixepoch', 'localtime') AS day,
               COUNT(*) AS salesCount,
               SUM(s.totalCents) AS revenueCents
        FROM sales s
        WHERE s.status = 'COMPLETED' AND s.completedAt BETWEEN :from AND :to
        GROUP BY day
        ORDER BY day
        """
    )
    fun observeDailyRevenue(from: Long, to: Long): Flow<List<DailyRevenue>>

    @Query(
        """
        SELECT c.name AS categoryName,
               SUM(si.quantity) AS unitsSold,
               SUM(si.lineTotalCents) AS revenueCents
        FROM sale_items si
        JOIN sales s ON s.id = si.saleId
        LEFT JOIN products p ON p.id = si.productId
        LEFT JOIN categories c ON c.id = p.categoryId
        WHERE s.status = 'COMPLETED' AND s.completedAt BETWEEN :from AND :to
        GROUP BY c.id
        ORDER BY revenueCents DESC
        """
    )
    fun observeRevenueByCategory(from: Long, to: Long): Flow<List<CategoryRevenue>>

    /** Gross profit = revenue - cost of goods, from the cost snapshot on each line. */
    @Query(
        """
        SELECT COALESCE(SUM(si.lineTotalCents - si.unitCostCents * si.quantity), 0)
        FROM sale_items si JOIN sales s ON s.id = si.saleId
        WHERE s.status = 'COMPLETED' AND s.completedAt BETWEEN :from AND :to
        """
    )
    fun observeGrossProfit(from: Long, to: Long): Flow<Long>
}
