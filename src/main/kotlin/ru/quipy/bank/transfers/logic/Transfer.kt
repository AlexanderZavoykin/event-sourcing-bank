package ru.quipy.bank.transfers.logic

import ru.quipy.bank.transfers.api.TransferAggregate
import ru.quipy.bank.transfers.api.TransferFailedEvent
import ru.quipy.bank.transfers.api.TransferInitiatedEvent
import ru.quipy.bank.transfers.api.TransferSucceededEvent
import ru.quipy.bank.transfers.logic.Transfer.TransferState.FAILED
import ru.quipy.bank.transfers.logic.Transfer.TransferState.PENDING
import ru.quipy.bank.transfers.logic.Transfer.TransferState.SUCCEEDED
import ru.quipy.core.annotations.StateTransitionFunc
import ru.quipy.domain.AggregateState
import java.math.BigDecimal
import java.util.UUID

class Transfer : AggregateState<UUID, TransferAggregate> {
    private lateinit var transferId: UUID
    lateinit var sourceAccountId: UUID
    lateinit var sourceBankAccountId: UUID
    lateinit var destinationAccountId: UUID
    lateinit var destinationBankAccountId: UUID
    lateinit var amount: BigDecimal
    var state: TransferState = PENDING

    override fun getId(): UUID = transferId

    fun initiateNewTransfer(
        sourceAccountId: UUID,
        sourceBankAccountId: UUID,
        destinationAccountId: UUID,
        destinationBankAccountId: UUID,
        amount: BigDecimal,
    ): TransferInitiatedEvent = TransferInitiatedEvent(
        transferId = UUID.randomUUID(),
        sourceAccountId = sourceAccountId,
        sourceBankAccountId = sourceBankAccountId,
        destinationAccountId = destinationAccountId,
        destinationBankAccountId = destinationBankAccountId,
        amount = amount,
    )

    fun succeeded(): TransferSucceededEvent = TransferSucceededEvent(transferId)

    fun failed(): TransferFailedEvent = TransferFailedEvent(transferId)


    @StateTransitionFunc
    fun initiateNewTransfer(event: TransferInitiatedEvent) {
        transferId = event.transferId
        sourceAccountId = event.sourceAccountId
        sourceBankAccountId = event.sourceBankAccountId
        destinationAccountId = event.destinationAccountId
        destinationBankAccountId = event.destinationBankAccountId
        amount = event.amount
    }

    @StateTransitionFunc
    fun fail(event: TransferFailedEvent) {
        state = FAILED
    }

    @StateTransitionFunc
    fun succeed(event: TransferSucceededEvent) {
        state = SUCCEEDED
    }

    enum class TransferState {
        PENDING,
        SUCCEEDED,
        FAILED,
    }
}