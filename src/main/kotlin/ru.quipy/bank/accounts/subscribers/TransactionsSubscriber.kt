package ru.quipy.bank.accounts.subscribers

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.quipy.bank.accounts.api.AccountAggregate
import ru.quipy.bank.transfers.api.TransferTransactionAggregate
import ru.quipy.bank.transfers.api.TransferTransactionCreatedEvent
import ru.quipy.bank.accounts.logic.Account
import ru.quipy.bank.transfers.api.TransactionConfirmedEvent
import ru.quipy.core.EventSourcingService
import ru.quipy.streams.AggregateSubscriptionsManager
import java.util.*
import javax.annotation.PostConstruct

@Component
class TransactionsSubscriber(
    private val subscriptionsManager: AggregateSubscriptionsManager,
    private val accountEsService: EventSourcingService<UUID, AccountAggregate, Account>
) {
    private val logger: Logger = LoggerFactory.getLogger(TransactionsSubscriber::class.java)

    @PostConstruct
    fun init() {
        subscriptionsManager.createSubscriber(TransferTransactionAggregate::class, "accounts::transaction-processing-subscriber") {
            `when`(TransferTransactionCreatedEvent::class) { event ->
                logger.info("Got transaction to process: $event")

                val transactionOutcome1 = accountEsService.update(event.sourceAccountId) { // todo sukhoa idempotence!
                    it.performTransferFrom(
                        event.sourceBankAccountId,
                        event.transferId,
                        event.transferAmount
                    )
                }

                val transactionOutcome2 = accountEsService.update(event.destinationAccountId) { // todo sukhoa idempotence!
                    it.performTransferTo(
                        event.destinationBankAccountId,
                        event.transferId,
                        event.transferAmount
                    )
                }

                logger.info("Transaction: ${event.transferId}. Outcomes: $transactionOutcome1, $transactionOutcome2")
            }
            `when`(TransactionConfirmedEvent::class) { event ->
                logger.info("Got transaction confirmed event: $event")

                val transactionOutcome1 = accountEsService.update(event.sourceAccountId) { // todo sukhoa idempotence!
                    it.processPendingTransaction(event.sourceBankAccountId, event.transferId)
                }

                val transactionOutcome2 = accountEsService.update(event.destinationAccountId) { // todo sukhoa idempotence!
                    it.processPendingTransaction(event.destinationBankAccountId, event.transferId)
                }

                logger.info("Transaction: ${event.transferId}. Outcomes: $transactionOutcome1, $transactionOutcome2")
            }
            // todo sukhoa bank account deleted event
        }
    }
}