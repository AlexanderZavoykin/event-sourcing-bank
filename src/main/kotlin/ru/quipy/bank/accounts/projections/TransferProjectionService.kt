package ru.quipy.bank.accounts.projections

import java.math.BigDecimal
import java.util.UUID

interface TransferProjectionService {

    fun save(
        transferId: UUID,
        sourceAccountId: UUID,
        sourceBankAccountId: UUID,
        destinationAccountId: UUID,
        destinationBankAccountId: UUID,
        amount: BigDecimal,
        state: TransferState,
    )

    fun updateState(transferId: UUID, state: TransferState)

    fun findById(transferId: UUID): TransferProjection?

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