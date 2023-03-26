package ru.quipy.bank.accounts.subscribers

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import ru.quipy.bank.accounts.api.AccountAggregate
import ru.quipy.bank.accounts.api.TransferDepositRejectedEvent
import ru.quipy.bank.accounts.api.TransferWithdrawPerformedEvent
import ru.quipy.bank.accounts.logic.Account
import ru.quipy.bank.saga.service.TransferCacheRepository
import ru.quipy.core.EventSourcingService
import ru.quipy.streams.AggregateSubscriptionsManager
import java.util.UUID
import javax.annotation.PostConstruct

@Component
class TransferTransactionAccountSubscriber(
    private val subscriptionsManager: AggregateSubscriptionsManager,
    private val transferCacheRepository: TransferCacheRepository,
    private val accountEsService: EventSourcingService<UUID, AccountAggregate, Account>
) {
    private val logger: Logger = LoggerFactory.getLogger(TransferTransactionAccountSubscriber::class.java)

    @PostConstruct
    fun init() {
        subscriptionsManager.createSubscriber(AccountAggregate::class, "accounts::transfer-transaction-processing-subscriber") {
            `when`(TransferWithdrawPerformedEvent::class) { event ->
                val transfer = transferCacheRepository.findByIdOrNull(event.transferId)!!

                accountEsService.update(transfer.destinationAccountId) {
                    it.performTransferDeposit(
                        transfer.transferId,
                        transfer.destinationBankAccountId,
                        transfer.amount,
                    )
                }
            }

            `when`(TransferDepositRejectedEvent::class) { event ->
                val transfer = transferCacheRepository.findByIdOrNull(event.transferId)!!

                accountEsService.update(transfer.sourceAccountId) {
                    it.rollbackTransferWithdraw(event.transferId, transfer.sourceBankAccountId, event.amount)
                }
            }
        }
    }
}