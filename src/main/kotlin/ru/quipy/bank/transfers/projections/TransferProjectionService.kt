package ru.quipy.bank.transfers.projections

import java.math.BigDecimal
import java.util.*

interface TransferProjectionService {

    fun save(transfer: TransferProjection)

    fun updateStateByTransferId(transferId: UUID, state: TransferState)

    fun findByTransferId(transferId: UUID): TransferProjection?

}

data class TransferProjection(
    val transferId: UUID,
    val sourceAccountId: UUID,
    val sourceBankAccountId: UUID,
    val destinationAccountId: UUID,
    val destinationBankAccountId: UUID,
    val amount: BigDecimal,
    var state: TransferState,
)

enum class TransferState {
    PENDING,
    SUCCEEDED,
    FAILED,
}