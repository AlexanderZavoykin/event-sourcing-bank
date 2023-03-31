package ru.quipy.bank.transfers.subscribers

import org.springframework.stereotype.Component
import ru.quipy.bank.transfers.api.TransferAggregate
import ru.quipy.bank.transfers.api.TransferFailedEvent
import ru.quipy.bank.transfers.api.TransferInitiatedEvent
import ru.quipy.bank.transfers.api.TransferSucceededEvent
import ru.quipy.bank.transfers.projections.TransferProjection
import ru.quipy.bank.transfers.projections.TransferProjectionService
import ru.quipy.bank.transfers.projections.TransferState
import ru.quipy.streams.AggregateSubscriptionsManager
import javax.annotation.PostConstruct

@Component
class TransferProjectionSubscriber(
    private val subscriptionsManager: AggregateSubscriptionsManager,
    private val transferProjectionService: TransferProjectionService,
) {

    @PostConstruct
    fun init() {
        subscriptionsManager.createSubscriber(
            TransferAggregate::class,
            "transfer-projections::transfer-processing-subscriber",
        ) {
            `when`(TransferInitiatedEvent::class) { event ->
                transferProjectionService.save(
                    TransferProjection(
                        transferId = event.transferId,
                        sourceAccountId = event.sourceAccountId,
                        sourceBankAccountId = event.sourceBankAccountId,
                        destinationAccountId = event.destinationAccountId,
                        destinationBankAccountId = event.destinationBankAccountId,
                        amount = event.amount,
                        state = TransferState.PENDING,
                    )
                )
            }
            `when`(TransferSucceededEvent::class) { event ->
                transferProjectionService.updateStateByTransferId(event.transferId, TransferState.SUCCEEDED)
            }
            `when`(TransferFailedEvent::class) { event ->
                transferProjectionService.updateStateByTransferId(event.transferId, TransferState.FAILED)
            }
        }
    }
}