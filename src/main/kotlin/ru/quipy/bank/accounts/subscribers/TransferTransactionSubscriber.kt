package ru.quipy.bank.accounts.subscribers

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import ru.quipy.bank.accounts.api.AccountAggregate
import ru.quipy.bank.accounts.api.TransferDepositPerformedEvent
import ru.quipy.bank.accounts.api.TransferDepositRejectedEvent
import ru.quipy.bank.accounts.api.TransferWithdrawPerformedEvent
import ru.quipy.bank.accounts.api.TransferWithdrawRejectedEvent
import ru.quipy.bank.accounts.logic.Account
import ru.quipy.bank.accounts.logic.TransferTransactionState.PERFORMED
import ru.quipy.bank.saga.service.TransferCacheRepository
import ru.quipy.bank.saga.service.TransferState.FAILED
import ru.quipy.bank.saga.service.TransferState.SUCCEEDED
import ru.quipy.core.EventSourcingService
import ru.quipy.streams.AggregateSubscriptionsManager
import java.util.UUID
import javax.annotation.PostConstruct

@Component
class TransferTransactionSubscriber(
    private val subscriptionsManager: AggregateSubscriptionsManager,
    private val transferCacheRepository: TransferCacheRepository,
    private val accountEsService: EventSourcingService<UUID, AccountAggregate, Account>
) {
    private val logger: Logger = LoggerFactory.getLogger(TransferTransactionSubscriber::class.java)

    @PostConstruct
    fun init() {
        subscriptionsManager.createSubscriber(AccountAggregate::class, "accounts::transfer-transaction-processing-subscriber") {
            `when`(TransferDepositRejectedEvent::class) { event ->
                val transfer = transferCacheRepository.findByIdOrNull(event.transferId)!!

                accountEsService.update(transfer.sourceAccountId) {
                    it.rollbackTransferWithdraw(event.transferId, transfer.sourceBankAccountId, event.amount)
                }

                transfer.state = FAILED
                transferCacheRepository.save(transfer)
            }
            `when`(TransferWithdrawRejectedEvent::class) { event ->
                val transfer = transferCacheRepository.findByIdOrNull(event.transferId)!!

                accountEsService.update(transfer.sourceAccountId) {
                    it.rollbackTransferDeposit(event.transferId, transfer.destinationBankAccountId, event.amount)
                }

                transfer.state = FAILED
                transferCacheRepository.save(transfer)
            }

            `when`(TransferDepositPerformedEvent::class) { event ->
                val transfer = transferCacheRepository.findByIdOrNull(event.transferId)!!

                val sourceAccount = accountEsService.getState(transfer.sourceAccountId)!!
                val sourceWithdrawState = sourceAccount.bankAccounts[transfer.sourceBankAccountId]!!
                    .transferTransactions[transfer.transferId]!!
                    .state

                if (sourceWithdrawState == PERFORMED) {
                    transfer.state = SUCCEEDED
                    transferCacheRepository.save(transfer)
                }
            }

            `when`(TransferWithdrawPerformedEvent::class) { event ->
                val transfer = transferCacheRepository.findByIdOrNull(event.transferId)!!

                val destinationAccount = accountEsService.getState(transfer.destinationAccountId)!!
                val destinationWithdrawState = destinationAccount.bankAccounts[transfer.destinationBankAccountId]!!
                    .transferTransactions[transfer.transferId]!!
                    .state

                if (destinationWithdrawState == PERFORMED) {
                    transfer.state = SUCCEEDED
                    transferCacheRepository.save(transfer)
                }
            }
        }
    }
}