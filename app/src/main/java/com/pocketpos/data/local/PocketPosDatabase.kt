package com.pocketpos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pocketpos.data.local.dao.AuditLogDao
import com.pocketpos.data.local.dao.BusinessSettingsDao
import com.pocketpos.data.local.dao.CashMovementDao
import com.pocketpos.data.local.dao.CategoryDao
import com.pocketpos.data.local.dao.CustomerDao
import com.pocketpos.data.local.dao.DiscountDao
import com.pocketpos.data.local.dao.ProductDao
import com.pocketpos.data.local.dao.PurchaseOrderDao
import com.pocketpos.data.local.dao.RefundDao
import com.pocketpos.data.local.dao.RegisterSessionDao
import com.pocketpos.data.local.dao.SaleDao
import com.pocketpos.data.local.dao.StockMovementDao
import com.pocketpos.data.local.dao.SupplierDao
import com.pocketpos.data.local.dao.TaxRateDao
import com.pocketpos.data.local.dao.UserDao
import com.pocketpos.data.local.entity.AuditLogEntity
import com.pocketpos.data.local.entity.BusinessSettingsEntity
import com.pocketpos.data.local.entity.CashMovementEntity
import com.pocketpos.data.local.entity.CategoryEntity
import com.pocketpos.data.local.entity.CustomerEntity
import com.pocketpos.data.local.entity.DiscountEntity
import com.pocketpos.data.local.entity.PaymentEntity
import com.pocketpos.data.local.entity.ProductEntity
import com.pocketpos.data.local.entity.PurchaseOrderEntity
import com.pocketpos.data.local.entity.PurchaseOrderItemEntity
import com.pocketpos.data.local.entity.RefundEntity
import com.pocketpos.data.local.entity.RefundItemEntity
import com.pocketpos.data.local.entity.RegisterSessionEntity
import com.pocketpos.data.local.entity.SaleEntity
import com.pocketpos.data.local.entity.SaleItemEntity
import com.pocketpos.data.local.entity.StockMovementEntity
import com.pocketpos.data.local.entity.SupplierEntity
import com.pocketpos.data.local.entity.TaxRateEntity
import com.pocketpos.data.local.entity.UserEntity

@Database(
    entities = [
        BusinessSettingsEntity::class,
        UserEntity::class,
        CategoryEntity::class,
        TaxRateEntity::class,
        ProductEntity::class,
        SupplierEntity::class,
        CustomerEntity::class,
        DiscountEntity::class,
        RegisterSessionEntity::class,
        CashMovementEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        PaymentEntity::class,
        RefundEntity::class,
        RefundItemEntity::class,
        StockMovementEntity::class,
        PurchaseOrderEntity::class,
        PurchaseOrderItemEntity::class,
        AuditLogEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class PocketPosDatabase : RoomDatabase() {

    abstract fun businessSettingsDao(): BusinessSettingsDao
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun taxRateDao(): TaxRateDao
    abstract fun productDao(): ProductDao
    abstract fun supplierDao(): SupplierDao
    abstract fun customerDao(): CustomerDao
    abstract fun discountDao(): DiscountDao
    abstract fun registerSessionDao(): RegisterSessionDao
    abstract fun cashMovementDao(): CashMovementDao
    abstract fun saleDao(): SaleDao
    abstract fun refundDao(): RefundDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun purchaseOrderDao(): PurchaseOrderDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: PocketPosDatabase? = null

        fun getInstance(context: Context): PocketPosDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PocketPosDatabase::class.java,
                    "pocketpos.db"
                )
                    .addCallback(SeedCallback)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}

/**
 * Runs once, the very first time the database file is created:
 * the single settings row + the Spanish IVA rates + a first category.
 * The first user (OWNER) is created by the app's first-run screen, because it needs a PIN.
 */
private object SeedCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)

        db.execSQL(
            """
            INSERT INTO business_settings
                (id, businessName, taxId, address, phone, email, currencyCode, receiptPrefix, nextReceiptNumber, receiptFooter)
            VALUES (1, 'Mi negocio', NULL, NULL, NULL, NULL, 'EUR', 'T', 1, '¡Gracias por su compra!')
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO tax_rates (name, rateBps, isDefault, isActive) VALUES
                ('IVA general 21%', 2100, 1, 1),
                ('IVA reducido 10%', 1000, 0, 1),
                ('IVA superreducido 4%', 400, 0, 1),
                ('Exento 0%', 0, 0, 1)
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO categories (name, colorHex, sortOrder, isActive) VALUES
                ('General', NULL, 0, 1)
            """.trimIndent()
        )
    }
}
