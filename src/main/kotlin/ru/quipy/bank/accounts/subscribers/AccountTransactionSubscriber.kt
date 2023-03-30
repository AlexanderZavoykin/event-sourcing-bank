package ru.quipy.bank.accounts.subscribers

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.quipy.bank.accounts.api.AccountAggregate
import ru.quipy.bank.accounts.api.TransferDepositRejectedEvent
import ru.quipy.bank.accounts.api.TransferWithdrawPerformedEvent
import ru.quipy.bank.accounts.logic.Account
import ru.quipy.bank.transfers.api.TransferAggregate
import ru.quipy.bank.transfers.logic.Transfer
import ru.quipy.core.EventSourcingService
import ru.quipy.streams.AggregateSubscriptionsManager
import java.util.UUID
import javax.annotation.PostConstruct

@Component("AccountAggregateAccountTransactionSubscriber")
class AccountTransactionSubscriber(
    private val subscriptionsManager: AggregateSubscriptionsManager,
    private val transferEsService: EventSourcingService<UUID, TransferAggregate, Transfer>,
    private val accountEsService: EventSourcingService<UUID, AccountAggregate, Account>,
) {
    private val logger: Logger = LoggerFactory.getLogger(AccountTransactionSubscriber::class.java)

    @PostConstruct
    fun init() {
        subscriptionsManager.createSubscriber(
            AccountAggregate::class,
            "accounts::account-transaction-processing-subscriber",
        ) {
            `when`(TransferWithdrawPerformedEvent::class) { event ->
                val transfer = transferEsService.getState(event.transferId)!!

                accountEsService.update(transfer.destinationAccountId) {
                    it.performTransferDeposit(
                        event.transferId,
                        transfer.destinationBankAccountId,
                        transfer.amount,
                    )
                }
            }

            `when`(TransferDepositRejectedEvent::class) { event ->
                val transfer = transferEsService.getState(event.transferId)!!

                accountEsService.update(transfer.sourceAccountId) {
                    it.rollbackTransferWithdraw(event.transferId, transfer.sourceBankAccountId, event.amount)
                }
            }
        }
    }
}