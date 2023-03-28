package ru.quipy.bank.accounts.projections

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class MongoDBTransferProjectionService(
    private val repository: TransferProjectionRepository,
) : TransferProjectionService {

    override fun save(
        transferId: UUID,
        sourceAccountId: UUID,
        sourceBankAccountId: UUID,
        destinationAccountId: UUID,
        destinationBankAccountId: UUID,
        amount: BigDecimal,
        state: TransferState,
    ) {
        repository.save(
            TransferProjectionDocument(
                transferId,
                sourceAccountId,
                sourceBankAccountId,
                destinationAccountId,
                destinationBankAccountId,
                amount,
                state,
            )
        )
    }

    override fun updateState(transferId: UUID, state: TransferState) {
        repository.findByIdOrNull(transferId)
            ?.let {
                it.state = state
                repository.save(it)
            }
    }

    override fun findById(transferId: UUID): TransferProjection? =
        repository.findByIdOrNull(transferId)?.toTransferProjection()

    private fun TransferProjectionDocument.toTransferProjection() =
        TransferProjection(
            transferId,
            sourceAccountId,
            sourceBankAccountId,
            destinationAccountId,
            destinationBankAccountId,
            amount,
            state,
        )
}

@Repository
interface TransferProjectionRepository : MongoRepository<TransferProjectionDocument, UUID>

@Document("transfer-projections")
data class TransferProjectionDocument(
    @Id
    val transferId: UUID,
    val sourceAccountId: UUID,
    val sourceBankAccountId: UUID,
    val destinationAccountId: UUID,
    val destinationBankAccountId: UUID,
    val amount: BigDecimal,
    var state: TransferState,
)

