package com.pocketpos.data.local.dev

import com.pocketpos.data.local.PocketPosDatabase
import com.pocketpos.data.local.entity.CategoryEntity
import com.pocketpos.data.local.entity.ProductEntity
import com.pocketpos.data.local.repository.ProductRepository

/**
 * ONLY FOR TESTING: fills an empty database with the same 4 products that used to be
 * hardcoded in InventoryScreen. Delete this file (and its call in InventoryViewModel)
 * when you have the real "Nuevo producto" form.
 */
object DevSeeder {

    suspend fun seedIfEmpty(db: PocketPosDatabase) {
        if (db.productDao().count() > 0) return

        val bebidas = categoryId(db, "Bebidas", 1)
        val panaderia = categoryId(db, "Panadería", 2)
        val repo = ProductRepository(db)

        listOf(
            ProductEntity(name = "Café Premium", categoryId = bebidas, priceCents = 250, stockQuantity = 24),
            ProductEntity(name = "Agua Mineral", categoryId = bebidas, priceCents = 120, stockQuantity = 4),
            ProductEntity(name = "Croissant", categoryId = panaderia, priceCents = 180, stockQuantity = 12),
            ProductEntity(name = "Zumo de naranja", categoryId = bebidas, priceCents = 295, stockQuantity = 0)
        ).forEach { repo.create(it, userId = null) }
    }

    private suspend fun categoryId(db: PocketPosDatabase, name: String, order: Int): Long =
        db.categoryDao().findByName(name)?.id
            ?: db.categoryDao().insert(CategoryEntity(name = name, sortOrder = order))
}
