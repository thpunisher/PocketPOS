package com.pocketpos.data.local.entity

/**
 * Every enum is stored by Room as TEXT (its name), so the DB stays readable
 * and adding a new value later needs no migration.
 */
enum class UserRole { OWNER, MANAGER, CASHIER }

enum class RegisterStatus { OPEN, CLOSED }

enum class CashMovementType { CASH_IN, CASH_OUT }

/** OPEN = parked / on-hold ticket, COMPLETED = paid, VOIDED = cancelled after payment. */
enum class SaleStatus { OPEN, COMPLETED, VOIDED }

enum class PaymentMethod { CASH, CARD, BIZUM, TRANSFER, OTHER }

enum class DiscountType { PERCENT, FIXED }

enum class StockMovementType { INITIAL, SALE, REFUND, VOID, PURCHASE, ADJUSTMENT, WASTE }

enum class PurchaseStatus { DRAFT, ORDERED, RECEIVED, CANCELLED }
