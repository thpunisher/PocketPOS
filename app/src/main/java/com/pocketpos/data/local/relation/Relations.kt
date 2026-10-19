package com.pocketpos.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.pocketpos.data.local.entity.CategoryEntity
import com.pocketpos.data.local.entity.CustomerEntity
import com.pocketpos.data.local.entity.PaymentEntity
import com.pocketpos.data.local.entity.ProductEntity
import com.pocketpos.data.local.entity.PurchaseOrderEntity
import com.pocketpos.data.local.entity.PurchaseOrderItemEntity
import com.pocketpos.data.local.entity.RefundEntity
import com.pocketpos.data.local.entity.RefundItemEntity
import com.pocketpos.data.local.entity.SaleEntity
import com.pocketpos.data.local.entity.SaleItemEntity
import com.pocketpos.data.local.entity.SupplierEntity
import com.pocketpos.data.local.entity.TaxRateEntity
import com.pocketpos.data.local.entity.UserEntity

/** What the inventory list needs: product + category name/color + tax rate. */
data class ProductWithDetails(
    @Embedded val product: ProductEntity,
    @Relation(parentColumn = "categoryId", entityColumn = "id")
    val category: CategoryEntity?,
    @Relation(parentColumn = "taxRateId", entityColumn = "id")
    val taxRate: TaxRateEntity?
)

/** A full ticket: lines, payments, customer and cashier. */
data class SaleWithDetails(
    @Embedded val sale: SaleEntity,
    @Relation(parentColumn = "id", entityColumn = "saleId")
    val items: List<SaleItemEntity>,
    @Relation(parentColumn = "id", entityColumn = "saleId")
    val payments: List<PaymentEntity>,
    @Relation(parentColumn = "customerId", entityColumn = "id")
    val customer: CustomerEntity?,
    @Relation(parentColumn = "userId", entityColumn = "id")
    val cashier: UserEntity
)

data class RefundWithItems(
    @Embedded val refund: RefundEntity,
    @Relation(parentColumn = "id", entityColumn = "refundId")
    val items: List<RefundItemEntity>
)

data class PurchaseOrderWithItems(
    @Embedded val order: PurchaseOrderEntity,
    @Relation(parentColumn = "supplierId", entityColumn = "id")
    val supplier: SupplierEntity,
    @Relation(parentColumn = "id", entityColumn = "purchaseOrderId")
    val items: List<PurchaseOrderItemEntity>
)
