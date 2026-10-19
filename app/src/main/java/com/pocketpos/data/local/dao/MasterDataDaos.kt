package com.pocketpos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.pocketpos.data.local.entity.BusinessSettingsEntity
import com.pocketpos.data.local.entity.CategoryEntity
import com.pocketpos.data.local.entity.CustomerEntity
import com.pocketpos.data.local.entity.DiscountEntity
import com.pocketpos.data.local.entity.SupplierEntity
import com.pocketpos.data.local.entity.TaxRateEntity
import com.pocketpos.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessSettingsDao {
    @Query("SELECT * FROM business_settings WHERE id = 1")
    fun observe(): Flow<BusinessSettingsEntity?>

    @Query("SELECT * FROM business_settings WHERE id = 1")
    suspend fun get(): BusinessSettingsEntity?

    @Upsert
    suspend fun save(settings: BusinessSettingsEntity)

    @Query("UPDATE business_settings SET nextReceiptNumber = nextReceiptNumber + 1 WHERE id = 1")
    suspend fun incrementReceiptCounter()
}

@Dao
interface UserDao {
    @Insert
    suspend fun insert(user: UserEntity): Long

    @Update
    suspend fun update(user: UserEntity)

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: Long): UserEntity?

    /** Login: the cashier types a PIN, the app hashes it and looks it up. */
    @Query("SELECT * FROM users WHERE pinHash = :pinHash AND isActive = 1 LIMIT 1")
    suspend fun findActiveByPinHash(pinHash: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY name")
    fun observeAll(): Flow<List<UserEntity>>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int
}

@Dao
interface CategoryDao {
    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Query("SELECT * FROM categories WHERE isActive = 1 ORDER BY sortOrder, name")
    fun observeActive(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sortOrder, name")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): CategoryEntity?

    @Query("SELECT COUNT(*) FROM products WHERE categoryId = :categoryId")
    suspend fun productCount(categoryId: Long): Int
}

@Dao
interface TaxRateDao {
    @Insert
    suspend fun insert(rate: TaxRateEntity): Long

    @Update
    suspend fun update(rate: TaxRateEntity)

    @Query("SELECT * FROM tax_rates WHERE id = :id")
    suspend fun getById(id: Long): TaxRateEntity?

    @Query("SELECT * FROM tax_rates WHERE isDefault = 1 AND isActive = 1 LIMIT 1")
    suspend fun getDefault(): TaxRateEntity?

    @Query("SELECT * FROM tax_rates WHERE isActive = 1 ORDER BY rateBps DESC")
    fun observeActive(): Flow<List<TaxRateEntity>>

    @Query("UPDATE tax_rates SET isDefault = 0")
    suspend fun clearDefault()

    @Query("UPDATE tax_rates SET isDefault = 1 WHERE id = :id")
    suspend fun markDefault(id: Long)

    @Transaction
    suspend fun setDefault(id: Long) {
        clearDefault()
        markDefault(id)
    }
}

@Dao
interface SupplierDao {
    @Insert
    suspend fun insert(supplier: SupplierEntity): Long

    @Update
    suspend fun update(supplier: SupplierEntity)

    @Query("SELECT * FROM suppliers WHERE id = :id")
    suspend fun getById(id: Long): SupplierEntity?

    @Query("SELECT * FROM suppliers WHERE isActive = 1 ORDER BY name")
    fun observeActive(): Flow<List<SupplierEntity>>
}

@Dao
interface CustomerDao {
    @Insert
    suspend fun insert(customer: CustomerEntity): Long

    @Update
    suspend fun update(customer: CustomerEntity)

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getById(id: Long): CustomerEntity?

    @Query(
        """
        SELECT * FROM customers
        WHERE isActive = 1
          AND (name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR email LIKE '%' || :query || '%')
        ORDER BY name
        """
    )
    fun observeSearch(query: String): Flow<List<CustomerEntity>>

    /** Lifetime spend of a customer (completed sales only). */
    @Query("SELECT COALESCE(SUM(totalCents), 0) FROM sales WHERE customerId = :customerId AND status = 'COMPLETED'")
    fun observeLifetimeSpend(customerId: Long): Flow<Long>
}

@Dao
interface DiscountDao {
    @Insert
    suspend fun insert(discount: DiscountEntity): Long

    @Update
    suspend fun update(discount: DiscountEntity)

    @Query("SELECT * FROM discounts WHERE id = :id")
    suspend fun getById(id: Long): DiscountEntity?

    @Query("SELECT * FROM discounts WHERE isActive = 1 ORDER BY name")
    fun observeActive(): Flow<List<DiscountEntity>>
}
