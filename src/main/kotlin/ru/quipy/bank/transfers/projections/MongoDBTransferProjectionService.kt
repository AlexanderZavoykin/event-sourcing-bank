package ru.quipy.bank.transfers.projections

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.*

@Service
class MongoDBTransferProjectionService(
    private val repository: TransferCacheRepository,
) : TransferProjectionService {

    override fun save(transfer: TransferProjection) {
        repository.insert(transfer.toDocument())
    }

    override fun updateStateByTransferId(transferId: UUID, state: TransferState) {
        repository.findById(transferId)
            .ifPresent { doc ->
                doc.state = state
                repository.save(doc)
            }
    }

    override fun findByTransferId(transferId: UUID): TransferProjection? =
        repository.findByIdOrNull(transferId)?.toProjection()

    private fun TransferProjection.toDocument(): TransferDocument =
        TransferDocument(
            transferId = transferId,
            sourceAccountId = sourceAccountId,
            sourceBankAccountId = sourceBankAccountId,
            destinationAccountId = destinationAccountId,
            destinationBankAccountId = destinationBankAccountId,
            amount = amount,
            state = state,
        )

    private fun TransferDocument.toProjection(): TransferProjection =
        TransferProjection(
            transferId = transferId,
            sourceAccountId = sourceAccountId,
            sourceBankAccountId = sourceBankAccountId,
            destinationAccountId = destinationAccountId,
            destinationBankAccountId = destinationBankAccountId,
            amount = amount,
            state = state,
        )

}

@Repository
interface TransferCacheRepository : MongoRepository<TransferDocument, UUID>

@Document("transfers-cache")
data class TransferDocument(
    @Id
    val transferId: UUID,
    val sourceAccountId: UUID,
    val sourceBankAccountId: UUID,
    val destinationAccountId: UUID,
    val destinationBankAccountId: UUID,
    val amount: BigDecimal,
    var state: TransferState,
)

