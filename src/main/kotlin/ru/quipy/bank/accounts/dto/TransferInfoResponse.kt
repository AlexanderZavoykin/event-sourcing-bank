package ru.quipy.bank.accounts.dto

import java.math.BigDecimal
import java.util.UUID

data class TransferInfoResponse(
    val transferId: UUID,
    val sourceBankAccountId: UUID,
    val destinationBankAccountId: UUID,
    val amount: BigDecimal,
    val state: TransferState,
)

enum class TransferState {
    PENDING,
    SUCCEEDED,
    FAILED,
}