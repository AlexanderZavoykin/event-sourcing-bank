package ru.quipy.bank.accounts.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.quipy.bank.accounts.api.AccountAggregate
import ru.quipy.bank.accounts.logic.Account
import ru.quipy.core.EventSourcingService
import ru.quipy.core.EventSourcingServiceFactory
import java.util.*

@Configuration
class AccountBoundedContextConfig {

    @Autowired
    private lateinit var eventSourcingServiceFactory: EventSourcingServiceFactory

    @Bean
    fun accountEsService(): EventSourcingService<UUID, AccountAggregate, Account> =
        eventSourcingServiceFactory.create()
}