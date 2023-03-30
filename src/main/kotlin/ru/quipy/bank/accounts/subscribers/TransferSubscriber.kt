package ru.quipy.bank.accounts.subscribers

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.quipy.bank.accounts.api.AccountAggregate
import ru.quipy.bank.accounts.logic.Account
import ru.quipy.bank.transfers.api.TransferAggregate
import ru.quipy.bank.transfers.api.TransferInitiatedEvent
import ru.quipy.core.EventSourcingService
import ru.quipy.streams.AggregateSubscriptionsManager
import java.util.UUID
import javax.annotation.PostConstruct

@Component
class TransferSubscriber(
    private val subscriptionsManager: AggregateSubscriptionsManager,
    private val accountEsService: EventSourcingService<UUID, AccountAggregate, Account>,
) {
    private val logger: Logger = LoggerFactory.getLogger(TransferSubscriber::class.java)

    @PostConstruct
    fun init() {
        subscriptionsManager.createSubscriber(
            TransferAggregate::class,
            "accounts::transfer-processing-subscriber",
        ) {
            `when`(TransferInitiatedEvent::class) { event ->
                accountEsService.update(event.sourceAccountId) {
                    it.performTransferWithdraw(
                        transferId = event.transferId,
                        bankAccountId = event.sourceBankAccountId,
                        amount = event.amount,
                    )
                }
            }
        }
    }
}