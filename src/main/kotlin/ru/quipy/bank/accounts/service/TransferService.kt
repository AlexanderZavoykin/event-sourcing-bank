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
        val srcBankAccount = bankAccountCacheRepository.findByIdOrNull(sourceBankAccountId)
            ?: throw IllegalArgumentException("Cannot create transaction. There is no source bank account: $sourceBankAccountId")

        val dstBankAccount = bankAccountCacheRepository.findByIdOrNull(destinationBankAccountId)
            ?: throw IllegalArgumentException("Cannot create transaction. There is no destination bank account: $destinationBankAccountId")

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
            )
        )

        logger.info(
            "Got transfer $transferId to process: amount $transferAmount " +
                    "from bank account $sourceBankAccountId to bank account $destinationBankAccountId"
        )

        accountEsService.update(sourceAccountId) {
            it.performTransferWithdraw(
                transferId = transferId,
                bankAccountId = sourceBankAccountId,
                amount = transferAmount,
            )
        }

        return TransferInfoResponse(
            transferId = transferId,
            sourceBankAccountId = sourceBankAccountId,
            destinationBankAccountId = destinationAccountId,
            amount = transferAmount,
            state = PENDING,
        )
    }
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
)