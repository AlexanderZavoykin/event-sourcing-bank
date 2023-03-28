package ru.quipy.bank.accounts.subscribers

import org.springframework.stereotype.Service
import ru.quipy.bank.accounts.api.AccountAggregate
import ru.quipy.bank.accounts.api.TransferDepositPerformedEvent
import ru.quipy.bank.accounts.api.TransferDepositRejectedEvent
import ru.quipy.bank.accounts.api.TransferWithdrawPerformedEvent
import ru.quipy.bank.accounts.api.TransferWithdrawRejectedEvent
import ru.quipy.bank.accounts.projections.TransferProjectionService
import ru.quipy.bank.accounts.projections.TransferState.FAILED
import ru.quipy.bank.accounts.projections.TransferState.PENDING
import ru.quipy.bank.accounts.projections.TransferState.SUCCEEDED
import ru.quipy.bank.saga.service.Transfer
import ru.quipy.bank.saga.service.TransferCacheRepository
import ru.quipy.streams.AggregateSubscriptionsManager
import java.util.UUID
import javax.annotation.PostConstruct

@Service
class TransferTransactionProjectionSubscriber(
    private val subscriptionsManager: AggregateSubscriptionsManager,
    private val transferProjectionService: TransferProjectionService,
    private val transferCacheRepository: TransferCacheRepository,
) {

    @PostConstruct
    fun init() {
        subscriptionsManager.createSubscriber(AccountAggregate::class, "accounts::transfer-transaction-projection-subscriber") {
            `when`(TransferWithdrawPerformedEvent::class) { event ->
                with(getTransfer(event.transferId)) {
                    transferProjectionService.save(
                        transferId,
                        sourceAccountId,
                        sourceBankAccountId,
                        destinationAccountId,
                        destinationBankAccountId,
                        amount,
                        PENDING
                    )
                }
            }

            `when`(TransferWithdrawRejectedEvent::class) { event ->
                with(getTransfer(event.transferId)) {
                    transferProjectionService.save(
                        transferId,
                        sourceAccountId,
                        sourceBankAccountId,
                        destinationAccountId,
                        destinationBankAccountId,
                        amount,
                        FAILED
                    )
                }
            }

            `when`(TransferDepositPerformedEvent::class) { event ->
                transferProjectionService.updateState(event.transferId, SUCCEEDED)
            }

            `when`(TransferDepositRejectedEvent::class) { event ->
                transferProjectionService.updateState(event.transferId, FAILED)
            }
        }
    }

    private fun getTransfer(transferId: UUID): Transfer = transferCacheRepository.findById(transferId).get()

}