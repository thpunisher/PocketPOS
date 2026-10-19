package com.pocketpos.data.local.repository

/**
 * Thrown for business-rule violations ("not enough stock", "register is closed"...).
 * The message is written in Spanish and can be shown to the cashier as-is
 * (e.g. in a Snackbar). Because repositories wrap work in Result, the UI does:
 *
 *     repo.checkout(request).onFailure { snackbar.show(it.message ?: "Error") }
 */
class PosException(message: String) : Exception(message)
