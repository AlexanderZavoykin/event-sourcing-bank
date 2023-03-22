package ru.quipy.bank.transfers.subscribers

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.quipy.bank.accounts.api.AccountAggregate
import ru.quipy.bank.accounts.api.TransferTransactionAcceptedEvent
import ru.quipy.bank.accounts.api.TransferTransactionDeclinedEvent
import ru.quipy.bank.accounts.api.TransferTransactionProcessedEvent
import ru.quipy.bank.accounts.api.TransferTransactionRollbackedEvent
import ru.quipy.bank.transfers.api.TransferTransactionAggregate
import ru.quipy.bank.transfers.logic.TransferTransaction
import ru.quipy.core.EventSourcingService
import ru.quipy.streams.AggregateSubscriptionsManager
import java.util.UUID
import javax.annotation.PostConstruct

@Component
class BankAccountsSubscriber(
    private val subscriptionsManager: AggregateSubscriptionsManager,
    private val transactionEsService: EventSourcingService<UUID, TransferTransactionAggregate, TransferTransaction>
) {
    private val logger: Logger = LoggerFactory.getLogger(BankAccountsSubscriber::class.java)

    @PostConstruct
    fun init() {
        subscriptionsManager.createSubscriber(AccountAggregate::class, "transactions::bank-accounts-subscriber") {
            `when`(TransferTransactionAcceptedEvent::class) { event ->
                transactionEsService.update(event.transactionId) {
                    it.processParticipantAccept(event.bankAccountId)
                }
            }
            `when`(TransferTransactionDeclinedEvent::class) { event ->
                transactionEsService.update(event.transactionId) {
                    it.processParticipantDecline(event.bankAccountId)
                }
            }
            `when`(TransferTransactionProcessedEvent::class) { event ->
                transactionEsService.update(event.transactionId) {
                    it.participantCommitted(event.bankAccountId)
                }
            }
            `when`(TransferTransactionRollbackedEvent::class) { event ->
                transactionEsService.update(event.transactionId) {
                    it.participantRollbacked(event.bankAccountId)
                }
            }
        }
    }
}