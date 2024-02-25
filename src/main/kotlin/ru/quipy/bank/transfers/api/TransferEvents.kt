package ru.quipy.bank.transfers.api

import ru.quipy.core.annotations.DomainEvent
import ru.quipy.domain.Event
import java.math.BigDecimal
import java.util.UUID

const val TRANSFER_INITIATED = "TRANSFER_INITIATED"
const val TRANSFER_SUCCEEDED = "TRANSFER_SUCCEEDED"
const val TRANSFER_FAILED = "TRANSFER_FAILED"

@DomainEvent(name = TRANSFER_INITIATED)
data class TransferInitiatedEvent(
    val transferId: UUID,
    val sourceAccountId: UUID,
    val sourceBankAccountId: UUID,
    val destinationAccountId: UUID,
    val destinationBankAccountId: UUID,
    val amount: BigDecimal,
) : Event<TransferAggregate>(
    name = TRANSFER_INITIATED,
)

@DomainEvent(name = TRANSFER_SUCCEEDED)
data class TransferSucceededEvent(
    val transferId: UUID,
) : Event<TransferAggregate>(
    name = TRANSFER_SUCCEEDED,
)

@DomainEvent(name = TRANSFER_FAILED)
data class TransferFailedEvent(
    val transferId: UUID,
) : Event<TransferAggregate>(
    name = TRANSFER_FAILED,
)