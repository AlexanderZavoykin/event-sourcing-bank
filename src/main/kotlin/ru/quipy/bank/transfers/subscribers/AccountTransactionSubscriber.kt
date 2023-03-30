package ru.quipy.bank.transfers.subscribers

import org.springframework.stereotype.Component
import ru.quipy.bank.accounts.api.AccountAggregate
import ru.quipy.bank.accounts.api.TransferDepositPerformedEvent
import ru.quipy.bank.accounts.api.TransferDepositRejectedEvent
import ru.quipy.bank.accounts.api.TransferWithdrawRejectedEvent
import ru.quipy.bank.transfers.api.TransferAggregate
import ru.quipy.bank.transfers.logic.Transfer
import ru.quipy.core.EventSourcingService
import ru.quipy.streams.AggregateSubscriptionsManager
import java.util.UUID
import javax.annotation.PostConstruct

@Component
class AccountTransactionSubscriber(
    private val subscriptionsManager: AggregateSubscriptionsManager,
    private val transferEsService: EventSourcingService<UUID, TransferAggregate, Transfer>,
) {

    @PostConstruct
    fun init() {
        subscriptionsManager.createSubscriber(
            AccountAggregate::class,
            "transfers::account-transaction-processing-subscriber",
        ) {
            `when`(TransferDepositPerformedEvent::class) { event ->
                transferEsService.update(event.transferId) {
                    it.succeeded()
                }
            }
            `when`(TransferWithdrawRejectedEvent::class) { event ->
                transferEsService.update(event.transferId) {
                    it.failed()
                }
            }
            `when`(TransferDepositRejectedEvent::class) { event ->
                transferEsService.update(event.transferId) {
                    it.failed()
                }
            }
        }
    }
}