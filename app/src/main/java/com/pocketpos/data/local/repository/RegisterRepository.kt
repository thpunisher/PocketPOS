package com.pocketpos.data.local.repository

import androidx.room.withTransaction
import com.pocketpos.data.local.PocketPosDatabase
import com.pocketpos.data.local.entity.AuditLogEntity
import com.pocketpos.data.local.entity.CashMovementEntity
import com.pocketpos.data.local.entity.CashMovementType
import com.pocketpos.data.local.entity.RegisterSessionEntity
import com.pocketpos.data.local.entity.RegisterStatus

/** Open / close the cash drawer and record petty-cash movements. */
class RegisterRepository(private val db: PocketPosDatabase) {

    private val sessionDao = db.registerSessionDao()
    private val movementDao = db.cashMovementDao()
    private val saleDao = db.saleDao()
    private val refundDao = db.refundDao()
    private val auditDao = db.auditLogDao()

    suspend fun open(userId: Long, openingFloatCents: Long): Result<Long> = runCatching {
        db.withTransaction {
            if (sessionDao.getOpen() != null) throw PosException("Ya hay una caja abierta")
            if (openingFloatCents < 0) throw PosException("El fondo de caja no es válido")

            val id = sessionDao.insert(
                RegisterSessionEntity(openedByUserId = userId, openingFloatCents = openingFloatCents)
            )
            auditDao.insert(
                AuditLogEntity(userId = userId, action = "REGISTER_OPEN", entityType = "register_session", entityId = id)
            )
            id
        }
    }

    suspend fun addCashMovement(
        sessionId: Long,
        userId: Long,
        type: CashMovementType,
        amountCents: Long,
        reason: String
    ): Result<Long> = runCatching {
        db.withTransaction {
            val session = sessionDao.getById(sessionId)
            if (session == null || session.status != RegisterStatus.OPEN) throw PosException("La caja está cerrada")
            if (amountCents <= 0) throw PosException("El importe no es válido")
            if (reason.isBlank()) throw PosException("Indica el motivo")

            movementDao.insert(
                CashMovementEntity(
                    sessionId = sessionId,
                    userId = userId,
                    type = type,
                    amountCents = amountCents,
                    reason = reason
                )
            )
        }
    }

    /**
     * Cash that SHOULD be in the drawer:
     * opening float + cash sales + cash in - cash out - cash refunds.
     */
    suspend fun expectedCash(sessionId: Long): Long {
        val session = sessionDao.getById(sessionId) ?: throw PosException("Caja no encontrada")
        return session.openingFloatCents +
            saleDao.cashPaymentsTotal(sessionId) +
            movementDao.totalBySession(sessionId, CashMovementType.CASH_IN) -
            movementDao.totalBySession(sessionId, CashMovementType.CASH_OUT) -
            refundDao.cashRefundsTotal(sessionId)
    }

    /** Closes the shift. difference = counted - expected (negative = missing money). */
    suspend fun close(
        sessionId: Long,
        userId: Long,
        countedCashCents: Long,
        note: String? = null
    ): Result<RegisterSessionEntity> = runCatching {
        db.withTransaction {
            val session = sessionDao.getById(sessionId)
            if (session == null || session.status != RegisterStatus.OPEN) throw PosException("La caja ya está cerrada")
            if (countedCashCents < 0) throw PosException("El importe contado no es válido")

            val expected = expectedCash(sessionId)
            val closed = session.copy(
                status = RegisterStatus.CLOSED,
                closedByUserId = userId,
                closedAt = System.currentTimeMillis(),
                expectedCashCents = expected,
                countedCashCents = countedCashCents,
                note = note
            )
            sessionDao.update(closed)
            auditDao.insert(
                AuditLogEntity(
                    userId = userId,
                    action = "REGISTER_CLOSE",
                    entityType = "register_session",
                    entityId = sessionId,
                    detail = "expected=$expected counted=$countedCashCents"
                )
            )
            closed
        }
    }
}
