package ru.quipy.bank.accounts.projections

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import ru.quipy.bank.accounts.api.AccountAggregate
import ru.quipy.bank.accounts.api.BankAccountCreatedEvent
import ru.quipy.streams.AggregateSubscriptionsManager
import java.util.*
import javax.annotation.PostConstruct

@Component
class BankAccountsExistenceCache(
    private val bankAccountCacheRepository: BankAccountCacheRepository,
    private val subscriptionsManager: AggregateSubscriptionsManager,
) {
    private val logger: Logger = LoggerFactory.getLogger(BankAccountsExistenceCache::class.java)

    @PostConstruct
    fun init() {
        subscriptionsManager.createSubscriber(AccountAggregate::class, "transactions::accounts-cache") {
            `when`(BankAccountCreatedEvent::class) { event ->
                bankAccountCacheRepository.save(BankAccount(event.bankAccountId, event.accountId)) // todo sukhoa idempotence!
                logger.info("Update bank account cache, create account ${event.accountId}-${event.bankAccountId}")
            }
        }
    }
}

@Repository
interface BankAccountCacheRepository : MongoRepository<BankAccount, UUID>

@Document("accounts-cache")
data class BankAccount(
    @Id
    val bankAccountId: UUID,
    var accountId: UUID
)

