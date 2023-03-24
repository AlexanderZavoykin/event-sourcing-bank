package ru.quipy.bank.saga.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import ru.quipy.bank.accounts.api.AccountAggregate
import ru.quipy.bank.accounts.dto.TransferInfoResponse
import ru.quipy.bank.accounts.dto.TransferState.PENDING
import ru.quipy.bank.accounts.logic.Account
import ru.quipy.bank.accounts.projections.BankAccountCacheRepository
import ru.quipy.core.EventSourcingService
import java.math.BigDecimal
import java.util.UUID

@Service
class TransferService(
    private val bankAccountCacheRepository: BankAccountCacheRepository,
    private val transferCacheRepository: TransferCacheRepository,
    private val accountEsService: EventSourcingService<UUID, AccountAggregate, Account>
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun initiateTransfer(
        sourceBankAccountId: UUID,
        destinationBankAccountId: UUID,
        transferAmount: BigDecimal,
    ): TransferInfoResponse {
        val srcBankAccount = bankAccountCacheRepository.findById(sourceBankAccountId).orElseThrow {
            IllegalArgumentException("Cannot create transaction. There is no source bank account: $sourceBankAccountId")
        }

        val dstBankAccount = bankAccountCacheRepository.findById(destinationBankAccountId).orElseThrow {
            IllegalArgumentException("Cannot create transaction. There is no destination bank account: $destinationBankAccountId")
        }

        val transferId = UUID.randomUUID()
        val sourceAccountId = srcBankAccount.accountId
        val destinationAccountId = dstBankAccount.accountId

        transferCacheRepository.save(
            Transfer(
                transferId = transferId,
                sourceAccountId = sourceAccountId,
                sourceBankAccountId = sourceBankAccountId,
                destinationAccountId = destinationAccountId,
                destinationBankAccountId = destinationBankAccountId,
                amount = transferAmount,
                state = TransferState.PENDING,
            )
        )

        logger.info(
            "Got transfer $transferId to process: amount $transferAmount " +
                    "from bank account $sourceBankAccountId to bank account $destinationBankAccountId"
        )

        val depositOutcome = accountEsService.update(destinationAccountId) {
            it.performTransferDeposit(
                transferId = transferId,
                bankAccountId = destinationBankAccountId,
                amount = transferAmount,
            )
        }

        val withdrawOutcome = accountEsService.update(sourceAccountId) {
            it.performTransferWithdraw(
                transferId = transferId,
                bankAccountId = sourceBankAccountId,
                amount = transferAmount,
            )
        }

        logger.info("Transfer: $transferId. Outcomes: $depositOutcome, $withdrawOutcome")

        return TransferInfoResponse(
            transferId = transferId,
            sourceBankAccountId = sourceBankAccountId,
            destinationBankAccountId = destinationAccountId,
            amount = transferAmount,
            state = PENDING,
        )
    }

    fun getTransfer(transferId: UUID): TransferInfoResponse =
        transferCacheRepository.findByIdOrNull(transferId)
            ?.let {
                TransferInfoResponse(
                    transferId = transferId,
                    sourceBankAccountId = it.sourceBankAccountId,
                    destinationBankAccountId = it.destinationAccountId,
                    amount = it.amount,
                    state = ru.quipy.bank.accounts.dto.TransferState.valueOf(it.state.name),
                )
            }?: throw IllegalArgumentException("Transfer $transferId is not found!")
    }

@Repository
interface TransferCacheRepository : MongoRepository<Transfer, UUID>

@Document("transfers-cache")
data class Transfer(
    @Id
    val transferId: UUID,
    val sourceAccountId: UUID,
    val sourceBankAccountId: UUID,
    val destinationAccountId: UUID,
    val destinationBankAccountId: UUID,
    val amount: BigDecimal,
    var state: TransferState,
)

enum class TransferState {
    PENDING,
    SUCCEEDED,
    FAILED,
}