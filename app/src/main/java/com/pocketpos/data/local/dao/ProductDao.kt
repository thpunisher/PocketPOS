package com.pocketpos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.pocketpos.data.local.entity.ProductEntity
import com.pocketpos.data.local.relation.ProductWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Insert
    suspend fun insert(product: ProductEntity): Long

    @Update
    suspend fun update(product: ProductEntity)

    @Query("SELECT COUNT(*) FROM products")
    suspend fun count(): Int

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<ProductEntity>

    /** Barcode scanner lookup. */
    @Query("SELECT * FROM products WHERE barcode = :barcode AND isActive = 1 LIMIT 1")
    suspend fun findByBarcode(barcode: String): ProductEntity?

    @Query("UPDATE products SET isActive = :active, updatedAt = :now WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean, now: Long = System.currentTimeMillis())

    /** Atomic stock change. Always pair it with a StockMovementEntity. */
    @Query("UPDATE products SET stockQuantity = stockQuantity + :delta, updatedAt = :now WHERE id = :id")
    suspend fun adjustStock(id: Long, delta: Int, now: Long = System.currentTimeMillis())

    @Query("UPDATE products SET costCents = :costCents, updatedAt = :now WHERE id = :id")
    suspend fun updateCost(id: Long, costCents: Long, now: Long = System.currentTimeMillis())

    // ---------- Inventory screen ----------

    /**
     * Search by name / barcode / SKU / category name (same behaviour as the current
     * hardcoded filter). categoryId = null means "all categories".
     */
    @Transaction
    @Query(
        """
        SELECT * FROM products
        WHERE isActive = 1
          AND (:categoryId IS NULL OR categoryId = :categoryId)
          AND (
                name LIKE '%' || :query || '%'
             OR barcode LIKE '%' || :query || '%'
             OR sku LIKE '%' || :query || '%'
             OR categoryId IN (SELECT id FROM categories WHERE name LIKE '%' || :query || '%')
          )
        ORDER BY name
        """
    )
    fun observeSearch(query: String, categoryId: Long? = null): Flow<List<ProductWithDetails>>

    @Transaction
    @Query("SELECT * FROM products WHERE id = :id")
    fun observeDetails(id: Long): Flow<ProductWithDetails?>

    @Query("SELECT COUNT(*) FROM products WHERE isActive = 1")
    fun observeActiveCount(): Flow<Int>

    /** "Stock bajo" card: 1..lowStockThreshold units left. */
    @Query(
        """
        SELECT COUNT(*) FROM products
        WHERE isActive = 1 AND trackStock = 1
          AND stockQuantity > 0 AND stockQuantity <= lowStockThreshold
        """
    )
    fun observeLowStockCount(): Flow<Int>

    /** "Agotados" card. */
    @Query("SELECT COUNT(*) FROM products WHERE isActive = 1 AND trackStock = 1 AND stockQuantity <= 0")
    fun observeOutOfStockCount(): Flow<Int>

    @Transaction
    @Query(
        """
        SELECT * FROM products
        WHERE isActive = 1 AND trackStock = 1 AND stockQuantity <= lowStockThreshold
        ORDER BY stockQuantity, name
        """
    )
    fun observeNeedsRestock(): Flow<List<ProductWithDetails>>

    /** Inventory value at cost (for reports). */
    @Query("SELECT COALESCE(SUM(stockQuantity * costCents), 0) FROM products WHERE isActive = 1 AND trackStock = 1 AND stockQuantity > 0")
    fun observeStockValueAtCost(): Flow<Long>
}
